import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import { aliases, mdi } from 'vuetify/iconsets/mdi'

export default createVuetify({
  components,
  directives,
  icons: {
    defaultSet: 'mdi',
    aliases,
    sets: { mdi },
  },
  theme: {
    defaultTheme: 'light',
    themes: {
      light: {
        colors: {
          primary: '#2c2416',
          secondary: '#8b7e6a',
          accent: '#c43d3d',
          error: '#c62828',
          info: '#6b5e4f',
          success: '#5b8c5a',
          warning: '#c43d3d',
          surface: '#ffffff',
          background: '#fdfaf6',
        },
      },
    },
  },
  defaults: {
    VCard: { elevation: 0, style: 'border: 1px solid #e8e0d5; border-radius: 8px;', class: 'pa-4' },
    VTextField: { variant: 'outlined', density: 'comfortable', color: '#c43d3d', hideDetails: 'auto' },
    VTextarea: { variant: 'outlined', density: 'comfortable', color: '#c43d3d', hideDetails: 'auto' },
    VBtn: { style: 'text-transform: none; letter-spacing: 0; font-weight: 400;' },
    VChip: { style: 'font-weight: 400;' },
  },
})
