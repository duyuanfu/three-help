/**
 * 后端接口地址配置
 *
 * 当前指向开发机的局域网 IP，微信开发者工具与手机真机都能用
 * （手机需与电脑连同一个 WiFi）。
 *
 * 必须设置：微信开发者工具「详情 → 本地设置」勾选
 *   「不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书」
 *
 * 电脑 IP 变了（重启路由 / 换网络）时，用 ipconfig 查新的 IPv4 改这里。
 * 若只想在模拟器里调试，也可用 http://127.0.0.1:8090/api —— 真机连不上。
 *
 * 正式上线（服务器 Docker 部署）：
 *   改成 https://api.你的域名.com/api —— 必须 HTTPS、已备案、只走 443，
 *   并在微信公众平台「开发管理 → 开发设置 → 服务器域名」同时配置
 *   request / uploadFile / downloadFile 三个合法域名。
 *   否则开发者工具勾了「不校验合法域名」时正常，真机却接口 403、图片全白。
 */
export const API_BASE_URL = 'https://bbb.trythis.pw/api';

/** 请求超时（毫秒） */
export const API_TIMEOUT = 15000;
