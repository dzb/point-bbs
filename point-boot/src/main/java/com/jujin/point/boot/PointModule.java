package com.jujin.point.boot;

import com.jujin.point.domain.AppContext;
import com.jujin.point.domain.entity.*;
import com.jujin.freeway.db.Database;
import com.jujin.freeway.db.Row;
import com.jujin.freeway.db.schema.PostgresDialect;
import com.jujin.freeway.db.schema.Schema;
import com.jujin.freeway.ioc.Binder;
import com.jujin.freeway.ioc.Container;
import com.jujin.freeway.ioc.ModuleEx;
import com.jujin.freeway.ioc.RuntimeHook;

import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.random.RandomGenerator;

/**
 * Primary bbs module — composes all feature modules and initializes AppContext.
 *
 * Pass this to Launcher.run(PointModule.class, args).
 * Freeway's DbModule and HttpModule are auto-discovered via ServiceLoader.
 */
public class PointModule implements ModuleEx {

    @Override
    public void bind(Binder binder) {
        // Sub-modules (ServiceModule, WebModule, AdminWebModule, CacheModule)
        // are auto-discovered via ServiceLoader — no manual bind() needed.

        // 1. Auto-migrate schema (freeway Schema.ensure)
        binder.contribute(RuntimeHook.class)
            .add("schema-migration", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var db = container.get(Database.class);
                    Schema.ensure(db, new PostgresDialect(), ALL_ENTITIES);
                }
                @Override
                public void stop(Container container) {}
            }).before("app-context-init");

        // 2. Seed default roles & permissions (idempotent)
        binder.contribute(RuntimeHook.class)
            .add("data-seed", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var db = container.get(Database.class);
                    long count = com.jujin.point.service.DbQuery.count(db,
                        "SELECT COUNT(*) AS cnt FROM bbs_permission", (Object[]) new Object[0]);
                    if (count == 0) {
                        long now = System.currentTimeMillis();
                        // Create default permissions
                        String[][] perms = {
                            {"admin", "admin", "管理员权限", "系统"},
                            {"admin", "topic:manage", "帖子管理", "内容"},
                            {"admin", "user:manage", "用户管理", "用户"},
                            {"admin", "config:manage", "配置管理", "系统"},
                            {"admin", "article:manage", "文章管理", "内容"},
                        };
                        for (String[] p : perms) {
                            db.execute("INSERT INTO bbs_permission (type, code, name, group_name, sort_no, status, create_time, update_time) VALUES (?,?,?,?,0,1,?,?)",
                                (Object) p[0], (Object) p[1], (Object) p[2], (Object) p[3], now, now);
                        }
                        // Create admin role
                        db.execute("INSERT INTO bbs_role (type, name, code, sort_no, status, create_time, update_time) VALUES (0,'管理员','admin',0,1,?,?)", now, now);
                        // Assign all permissions to admin role
                        var rows = db.query("SELECT id FROM bbs_permission").list(Row.class);
                        for (var r : rows) {
                            long pid = r.longVal("id");
                            db.execute("INSERT INTO bbs_role_permission (role_id, permission_id, create_time) VALUES (1,?,?)", pid, now);
                        }
                    }
                }
                @Override
                public void stop(Container container) {}
            }).before("app-context-init");

        // 2b. Seed dev test data (idempotent — only runs when no users exist)
        binder.contribute(RuntimeHook.class)
            .add("dev-data-seed", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    var db = container.get(Database.class);
                    long userCount = com.jujin.point.service.DbQuery.count(db,
                        "SELECT COUNT(*) AS cnt FROM bbs_user", (Object[]) new Object[0]);
                    if (userCount > 0) return;

                    long t = System.currentTimeMillis();
                    String pw = hashPw("123456");

                    // --- test users ---
                    record U(String n, String u, String a, String d) {}
                    U[] users = {
                        new U("墨客", "moke",  "https://api.dicebear.com/9.x/notionists/svg?seed=Moke",  "静心写字，安然读书。一纸墨痕，半窗月明。"),
                        new U("听雨", "tingyu","https://api.dicebear.com/9.x/notionists/svg?seed=Tingyu","夜雨敲窗，煮茶读帖。人间有味是清欢。"),
                        new U("竹心", "zhuxin","https://api.dicebear.com/9.x/notionists/svg?seed=Zhuxin","竹影扫阶尘不动，月轮穿沼水无痕。"),
                        new U("山客", "shanke","https://api.dicebear.com/9.x/notionists/svg?seed=Shanke","山中无历日，寒尽不知年。"),
                        new U("素笺", "sujian","https://api.dicebear.com/9.x/notionists/svg?seed=Sujian","素笺淡墨，写不尽人间烟火。"),
                    };
                    for (var u : users) db.execute(
                        "INSERT INTO bbs_user (create_time,update_time,nickname,username,password,avatar,description,email_verified,score,exp,level,status,topic_count,comment_count,follow_count,fans_count,forbidden_end_time) VALUES (?,?,?,?,?,?,?,true,0,0,1,1,0,0,0,0,0)",
                        t, t, u.n(), u.u(), pw, u.a(), u.d());

                    // --- categories ---
                    record C(long pid, String n, String ty, String d, String l, int s) {}
                    C[] cats = { new C(0,"墨池","free","分享与讨论，以文会友。","📝",1),
                                 new C(0,"问答","qa",  "疑义相与析，奇文共欣赏。","❓",2),
                                 new C(0,"茶馆","free","闲话二三，清茶一杯。","🍵",3)};
                    for (var c : cats) db.execute(
                        "INSERT INTO bbs_category (parent_id,name,type,description,logo,sort_no,status,create_time) VALUES (?,?,?,?,?,?,1,?)",
                        c.pid(), c.n(), c.ty(), c.d(), c.l(), c.s(), t);

                    // --- topic content (Java text blocks, no escaping) ---
                    var c1 = """
                            最近重新翻看钱锺书的《围城》，发现年轻时看的是故事，中年再看，字里行间都是人生经验。

                            ## 几点随感

                            1. **幽默背后的冷眼** — 钱的讽刺不是刻薄，而是一种看透之后的悲悯。
                            2. **"围城"隐喻的扩展** — 不止婚姻，职业、社交圈、甚至阅读本身，都可能是围城。
                            3. **语言的密度** — 几乎每句话都有典故或双关，这是现代散文少见的密度。

                            > 钱锺书在序里说："我们常把自己的写作冲动误认为自己的写作才能。"这句话对每个写字的人都是一种警醒。

                            大家最近在读什么书？欢迎分享。""";
                    var c3 = """
                            在搭建这个论坛的过程中，一直在思考一个问题：**如何让长文在屏幕上的阅读体验接近纸质书？**

                            目前尝试的方案：
                            - 正文字号 17px，行高 1.9
                            - 使用衬线字体（Noto Serif SC）做标题
                            - 正文使用系统无衬线字体
                            - 最大宽度控制在 680px

                            几个待解决的问题：
                            1. 移动端要不要缩小字号？
                            2. 代码块在衬线阅读流里怎么协调？
                            3. 图片和文字的间距怎么定？

                            欢迎有排版经验的朋友给建议 🙏""";
                    var c6 = """
                            最近在做一个跟文字相关的项目，想找一些**中文排版**的优秀参考。

                            具体要求：
                            - 正文字号在 16-18px 之间
                            - 行高舒适（1.7-2.0 之间）
                            - 衬线或无衬线均可
                            - 最好有移动端的适配方案

                            目前参考了：即刻、豆瓣、Readmoo。还有其他推荐吗？""";
                    var c9 = """
                            ## 写作

                            在社区写了 47 篇帖子，累计大概 5 万字。从最初的磕磕绊绊到现在可以随手写几百字，最大的进步可能不是文笔，而是**敢写了**。

                            ## 阅读

                            读完 31 本书。推荐三本：
                            1. 《重走》杨潇 — 在西南联大撤退路线上行走的游记
                            2. 《羊道》李娟 — 新疆游牧生活的记录，文字干净极了
                            3. 《打开》周濂 — 哲学入门，适合非专业人士

                            ## 风景

                            去了云南、川西、皖南。最美的时刻都是在路上——而不是在目的地。

                            希望明年继续读万卷书，行万里路 🌿""";

                    record TP(long u, int ty, long ci, String ti, String co, int qa) {}
                    TP[] topics = {
                        new TP(1,0,1,"读书札记：重读《围城》的一种角度", c1, 0),
                        new TP(2,1,2,null, "夜读时分，窗外雨声淅沥。忽然想起苏东坡的\"一蓑烟雨任平生\"，那种豁达，大概是要经历很多才能学会的。", 0),
                        new TP(3,0,3,"技术讨论：关于 BBS 长文排版的最佳实践", c3, 0),
                        new TP(4,1,1,null, "下班路上看到一只橘猫趴在书店门口的台阶上晒太阳，忽然觉得这种慵懒的从容，就是我理想的生活状态 😸", 0),
                        new TP(1,1,2,null, "今日练字打卡。临《兰亭序》第三遍，终于把\"永和九年\"写得有几分王羲之的意思了。老师说写字不在多，在精。每天二十个字，认认真真写，比敷衍一百个强。", 0),
                        new TP(2,0,3,"提问：有没有好的中文排版参考？", c6, 2),
                        new TP(5,1,1,null, "泡了一壶铁观音，翻开一本旧书。纸张微微泛黄，有一股陈年的纸墨香。这种气味，电子书永远替代不了。", 0),
                        new TP(3,1,2,null, "分享一个有意思的发现：写字的时候放白噪音（雨声、海浪声）比听歌更专注。可能因为人声和旋律会占用大脑的语言处理区域，而自然声音不会。\n\n大家码字/写代码的时候听什么？", 0),
                        new TP(5,0,1,"年终总结：这一年写的字、读的书、见的风景", c9, 0),
                    };
                    for (var tp : topics) {
                        db.execute("INSERT INTO bbs_topic (create_time,type,category_id,user_id,title,content,status,qa_status,view_count,comment_count,like_count,last_comment_time,bounty_score,solved_at,recommend,recommend_time,sticky,sticky_time) VALUES (?,?,?,?,?,?,1,?,0,0,0,?,0,0,false,0,false,0)",
                            t, tp.ty(), tp.ci(), tp.u(), tp.ti(), tp.co(),
                            tp.qa() == 2 ? "unsolved" : null, t);
                        try { Thread.sleep(2); } catch (InterruptedException ignored) {}
                    }

                    // --- comments (on topics) ---
                    record CM(long u, String et, long ei, String co, long qi) {}
                    CM[] comments = {
                        new CM(1,"topic",1,"写得真好。我也特别喜欢《围城》，每年都会翻一遍，每次都能读出不一样的东西。",0),
                        new CM(3,"topic",1,"推荐钱锺书的《管锥编》，虽然难读，但里面的典故梳理非常震撼。",0),
                        new CM(4,"topic",1,"说到幽默中的冷眼，想起鲁迅。鲁迅是热肠的冷眼，钱锺书是智者的冷眼。各有味道。",0),
                        new CM(2,"topic",3,"建议正文直接用 16px，17px 在移动端有点大。另外行高 1.7 比 1.9 更舒服。",0),
                        new CM(5,"topic",3,"代码块可以用 14px 等宽字体，跟正文拉开层级就很自然了。",0),
                        new CM(1,"topic",3,"移动端不要缩小字号，但要保证最大宽度。16-17px 在手机上也很舒服。",0),
                        new CM(3,"topic",6,"即刻的排版确实不错。另外推荐少数派和 Typography 正刊。",0),
                        new CM(1,"topic",9,"47 篇帖子、31 本书、三个省份 —— 这真是一个丰盛的年头。佩服你的坚持 ✨",0),
                        new CM(2,"topic",9,"李娟的文字真的干净。推荐她的《冬牧场》，比《羊道》还纯粹。",0),
                        new CM(4,"topic",9,"明年一起去川西吧！我有徒步路线想分享。",0),
                        new CM(3,"topic",4,"橘猫是最懂生活的哲学家 😂 我家楼下也有一只，每天固定时间在同一个位置晒太阳。",0),
                        new CM(5,"topic",2,"写字和做事的道理一样。少即是多，慢即是快。",0),
                    };
                    for (var c : comments)
                        db.execute("INSERT INTO bbs_comment (create_time,user_id,entity_type,entity_id,content,quote_id,like_count,comment_count,status) VALUES (?,?,?,?,?,?,0,0,1)",
                            t, c.u(), c.et(), c.ei(), c.co(), c.qi());

                    // --- article ---
                    var artContent = """
                            ## 缘起

                            两年前的一个雨夜，我在整理电脑文件夹时，发现了一个名为"碎片"的文档。打开一看，是过去十年间随手写下的几百条笔记——有读书摘抄，有偶然的感悟，有和朋友争论后的反思。

                            它们东一句西一句，不成体系，但读起来却让我看到了一个更真实的自己。

                            > 碎片比完整更诚实。

                            ## 为什么要保存碎片

                            完整的思想往往是事后加工过的。就像拍照前的摆拍，你知道有人在看，所以你调整姿态、管理表情。

                            但碎片不同。碎片是你来不及修饰的瞬间——

                            - 地铁上突然想到的一句话
                            - 失眠时写在手机备忘录里的几行字
                            - 读到某段话时在页边画的一颗星
                            - 和朋友聊到深夜时脱口而出的某个观点

                            这些碎片没有包装，反而保存了最原始的能量。

                            ## 怎样收集碎片

                            我的方法很简单：

                            1. **降级记录门槛** — 不需要完整句子，几个关键词就够了。
                            2. **不分类** — 分类会打断记录。先记下来，整理是以后的事。
                            3. **定期重读** — 每月花一小时翻看碎片列表，常常会有惊喜的连接。

                            ## 碎片的连接

                            最有意思的是，当你积累了一定数量的碎片后，它们会自己产生连接。

                            去年我在笔记里看到同一天写下的两句话：

                            > "写字的难点不是有话要说，而是知道什么不必说。"

                            > "好的设计是做减法，好的写作也是。"

                            这两句话被同一本书的不同段落触发，但本质上在说同一件事。如果当时我试图"写一篇文章"，可能永远写不出这么直接的表达。

                            ## 最后

                            如果你也有随手记东西的习惯，不妨翻出来看看。那些零散的碎片，可能是你最真实的思考地图。

                            欢迎在评论区分享你的碎片收集方法。""";
                    db.execute("INSERT INTO bbs_article (create_time,update_time,user_id,title,summary,content,content_type,status,view_count,comment_count,like_count) VALUES (?,?,?,?,?,?,?,1,0,0,0)",
                        t, t, 1L, "关于保存「碎片想法」的一些思考",
                        "碎片比完整更诚实。在整理了几百条随手笔记之后，我发现那些不经修饰的片段，才是最有价值的思考材料。",
                        artContent, "markdown");

                    // --- article comments ---
                    CM[] artComments = {
                        new CM(2,"article",1,"说得太好了。我也有一个类似的“灵感抽屉”，每次打开都有惊喜。",0),
                        new CM(3,"article",1,"“碎片比完整更诚实” — 这句话值得写在便签上贴在显示器旁边。",0),
                        new CM(4,"article",1,"想问一下，你是用哪个工具来做碎片管理的？我试过 Notion、Obsidian 和 flomo，各有优劣。",0),
                    };
                    for (var c : artComments)
                        db.execute("INSERT INTO bbs_comment (create_time,user_id,entity_type,entity_id,content,quote_id,like_count,comment_count,status) VALUES (?,?,?,?,?,?,0,0,1)",
                            t+20, c.u(), c.et(), c.ei(), c.co(), c.qi());

                    // --- user follows ---
                    long[][] follows = {{1,2},{1,3},{1,4},{2,1},{2,3},{3,1},{3,5},{4,1},{4,2},{4,5},{5,1},{5,3}};
                    for (var f : follows) db.execute("INSERT INTO bbs_user_follow (user_id,other_id,status,create_time) VALUES (?,?,1,?)", f[0], f[1], t);

                    // --- likes ---
                    Object[][] likes = {{1L,"topic",2L},{1L,"topic",3L},{2L,"topic",1L},{2L,"topic",9L},{3L,"topic",1L},{3L,"topic",6L},{4L,"topic",1L},{4L,"topic",9L},{5L,"topic",1L},{5L,"topic",4L}};
                    for (var l : likes) db.execute("INSERT INTO bbs_user_like (user_id,entity_type,entity_id,create_time) VALUES (?,?,?,?)", l[0], l[1], l[2], t);

                    // --- fix user counts ---
                    db.execute("UPDATE bbs_user SET topic_count = (SELECT COUNT(*) FROM bbs_topic WHERE user_id = bbs_user.id AND status = 1)");
                    db.execute("UPDATE bbs_user SET comment_count = (SELECT COUNT(*) FROM bbs_comment WHERE user_id = bbs_user.id AND status = 1)");
                    db.execute("UPDATE bbs_user SET follow_count = (SELECT COUNT(*) FROM bbs_user_follow WHERE user_id = bbs_user.id AND status = 1)");
                    db.execute("UPDATE bbs_user SET fans_count = (SELECT COUNT(*) FROM bbs_user_follow WHERE other_id = bbs_user.id AND status = 1)");

                    System.out.println("[dev-data] Seeded " + users.length + " users, " + cats.length + " categories, " + topics.length + " topics, " + (comments.length + artComments.length) + " comments, 1 article");
                }
                @Override
                public void stop(Container container) {}
            }).before("app-context-init");

        // 3. Initialize AppContext AFTER all services are bound
        //    (runs before HTTP server starts accepting requests)
        binder.contribute(RuntimeHook.class)
            .add("app-context-init", new RuntimeHook() {
                @Override
                public void start(Container container) {
                    AppContext.init(container);
                }

                @Override
                public void stop(Container container) {}
            }).before("freeway.http.server");
    }

    /** SHA-256 + random salt, same format as UserService.hashPassword. */
    static String hashPw(String password) {
        try {
            var rng = RandomGenerator.getDefault();
            byte[] salt = new byte[16];
            rng.nextBytes(salt);
            var md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hash = md.digest(password.getBytes("UTF-8"));
            return HexFormat.of().formatHex(salt) + ":" + HexFormat.of().formatHex(hash);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** All entity classes for Schema.ensure auto-migration. */
    static final Class<?>[] ALL_ENTITIES = {
        User.class, Topic.class, Comment.class, Article.class, Category.class,
        Tag.class, TopicTag.class, ArticleTag.class,
        Vote.class, VoteOption.class, VoteRecord.class,
        UserLike.class, Favorite.class, UserFollow.class, Message.class,
        CheckIn.class, UserScoreLog.class, UserExpLog.class, OperateLog.class,
        UserTaskEvent.class, UserTaskLog.class, TaskConfig.class,
        Badge.class, UserBadge.class, LevelConfig.class,
        SysConfig.class, Link.class, ForbiddenWord.class,
        Attachment.class, AttachmentDownloadLog.class,
        UserFeed.class, UserReport.class,
        EmailCode.class, EmailLog.class, SmsCode.class,
        UserToken.class, ThirdUser.class,
        Role.class, Permission.class, RolePermission.class, UserRole.class,
        DictType.class, Dict.class
    };
}
