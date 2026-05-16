// PostCSS configuration for the supported modern browser targets.
module.exports = {
  plugins: {
    autoprefixer: {
      grid: true,
      flexbox: 'no-2009',
    },
  },
}
