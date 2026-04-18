// Win7 兼容：PostCSS 配置，自动添加浏览器前缀
module.exports = {
  plugins: {
    autoprefixer: {
      grid: true,
      flexbox: 'no-2009',
    },
  },
}
