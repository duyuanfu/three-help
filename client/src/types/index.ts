/**
 * 帮帮帮小程序核心领域实体与类型定义
 */

// 用户角色与身份
export type UserRole = 'elder' | 'volunteer';

export interface UserProfile {
  id: string;
  name: string;
  avatar: string;
  gender: '男' | '女';
  age: number;
  phone: string;
  isRealName: boolean;
  identityTag: string; // 如 "@学生"、"@村支书"、"@村委会干事"
  occupation: string;  // 职业，如 "退休职工"、"华侨大学学生"
  address: string;     // 常用地址，如 "福建省泉州市丰泽区凤阳小区8号楼302"
  organization: string;// 所属组织/社区，如 "丰泽社区居委会"
  points: number;      // 金币/积分余额
  /** 是否管理员（由服务端下发，决定是否显示管理后台入口） */
  isAdmin?: boolean;
}

// 互助订单
export type OrderStatus = 'pending' | 'accepted' | 'in_progress' | 'completed' | 'cancelled';
export type OrderCategory = 
  | 'transport'    // 出行接送
  | 'medical'      // 出行就医/陪诊
  | 'shopping'     // 购物陪伴
  | 'delivery'     // 物品代买/帮抢鸡蛋
  | 'cleaning'     // 家政服务/上门打扫/上门做饭
  | 'consult'      // 线上咨询
  | 'teach_phone'  // 上门教手机
  | 'repair';      // 上门修理

export interface HelperInfo {
  id: string;
  name: string;
  tag: string;       // 如 "@学生"、"@村干部"
  avatar: string;
  phone: string;
}

export interface HelpOrder {
  id: string;
  creatorId: string;
  creatorName: string;
  creatorAvatar: string;
  creatorTag?: string;
  category: OrderCategory;
  title: string;           // 任务简称，如 "医院陪诊"、"帮抢鸡蛋"、"教会手机"
  content: string;         // 详细描述，如 "需要有人来陪散心"
  appointTime: string;     // 约定时间，如 "2026/06/08 14:00-17:00"
  address: string;         // 约定地点，如 "凤阳小区8号楼"
  bounty: number;          // 下单金额 (金币/元)，如 30
  status: OrderStatus;
  statusText?: string;     // 状态描述，如 "帮手即将到达"、"已匹配到 刘小燕 帮手"
  helper?: HelperInfo;     // 接单人
  createdAt: string;       // 创建时间
  completedAt?: string;    // 完成时间
  review?: {
    stars: number;
    comment: string;
  };
  /** 是否是我发布的 */
  isMine?: boolean;
  /** 是否是我接的单 */
  isHelper?: boolean;
}

// 通讯录联系人
export interface ContactItem {
  id: string;
  name: string;
  avatar: string;
  tag?: string;            // 职业标签，如 "@退休教师"、"@学生"
  letter: string;          // 拼音首字母 A-Z，用于通讯录索引排序
  phone: string;
  isFriend: boolean;
  /** 若手机号对应已注册用户，则关联对方账号ID（用于真实聊天） */
  targetUserId?: string;
}

// 消息与聊天
export type MessageType = 'text' | 'image' | 'voice' | 'card' | 'file' | 'phone_call' | 'video_call';

export interface ChatMessage {
  id: string;
  sessionId: string;
  senderId: string;
  receiverId: string;
  type: MessageType;
  content: string;         // 文字内容或图片路径
  duration?: number;       // 语音时长秒数
  audioPath?: string;      // 语音临时文件路径
  createdAt: string;       // 格式化时间，如 "19:58"
  timestamp: number;
}

export interface ChatSession {
  sessionId: string;
  targetUser: {
    id: string;
    name: string;
    avatar: string;
    tag?: string;
  };
  lastMessage: string;
  lastTime: string;
  unreadCount: number;
  relatedOrderId?: string; // 关联的求助订单 ID
}

// 广场动态帖子
export interface PostItem {
  id: string;
  authorId: string;
  authorName: string;
  authorAvatar: string;
  authorTag?: string;
  content: string;
  images?: string[];
  likeCount: number;
  commentCount: number;
  timeAgo: string;         // 如 "1分钟前"、"昨天"
  isLiked?: boolean;
}

// 帖子评论
export interface CommentItem {
  id: string;
  postId: string;          // 关联的动态 id
  userName: string;
  avatar: string;
  content: string;
  /** 评论者账号ID */
  authorId?: string;
}

// 积分商城商品
export interface MallGoods {
  id: string;
  name: string;            // 如 "鸡蛋/盒"、"袋子/捆"、"牙膏/盒"、"纸巾/盒"
  price: number;           // 所需金币，如 1000
  image: string;
  unit: string;
  stock: number;
}

export interface CartItem {
  goods: MallGoods;
  count: number;
}

// 兑换订单与物流追踪节点
export interface LogisticsStep {
  time: string;            // 如 "2025/08/23 20:00"
  title: string;           // "已发货"、"转站中"、"已到达"
  subtitle?: string;       // "华大接驳点"
  code?: string;           // "取件码 70-2-0029"
  isCurrent: boolean;
}

export interface ExchangeOrder {
  id: string;
  orderNo: string;         // 运单号，如 "75124398600122"
  goodsName: string;       // 如 "鸡蛋一盒"
  goodsImage: string;
  quantity: number;
  pointsCost: number;
  address: string;
  expressCompany: string;  // 如 "中通快递"
  senderCity: string;      // "泉州市 帮帮超市"
  receiverCity: string;    // "泉州市 张冬梅"
  createdAt: string;
  steps: LogisticsStep[];
}

// 全局设置
export interface AppSetting {
  /** 服务端单行表主键，首次写入由后端补齐 */
  id?: number;
  fontScaleLevel: number;  // 0: 小, 1: 标准(默认), 2: 大, 3: 超大
  fontScale: number;       // 0.85, 1.0, 1.15, 1.3
}
