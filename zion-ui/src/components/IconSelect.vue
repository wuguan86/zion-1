<template>
  <n-popover trigger="click" placement="bottom" :width="400">
    <template #trigger>
      <n-input
        :value="modelValue"
        placeholder="请选择图标"
        readonly
        style="cursor: pointer"
      >
        <template #prefix v-if="modelValue">
          <n-icon :size="18">
            <component :is="getIconComponent(modelValue)" />
          </n-icon>
        </template>
        <template #suffix>
          <n-icon :size="14" style="cursor: pointer" @click.stop="handleClear" v-if="modelValue">
            <CloseOutline />
          </n-icon>
        </template>
      </n-input>
    </template>
    <div class="icon-select-container">
      <n-input
        v-model:value="searchText"
        placeholder="搜索图标"
        clearable
        size="small"
        style="margin-bottom: 12px"
      />
      <n-scrollbar style="max-height: 300px">
        <div class="icon-grid">
          <div
            v-for="icon in filteredIcons"
            :key="icon.name"
            class="icon-item"
            :class="{ active: modelValue === icon.name }"
            @click="handleSelect(icon.name)"
            :title="icon.name"
          >
            <n-icon :size="22">
              <component :is="icon.component" />
            </n-icon>
            <span class="icon-name">{{ icon.label }}</span>
          </div>
        </div>
      </n-scrollbar>
    </div>
  </n-popover>
</template>

<script setup lang="ts">
import { ref, computed, markRaw, type Component } from 'vue'
import { NIcon, NInput, NPopover, NScrollbar } from 'naive-ui'
import {
  HomeOutline,
  SettingsOutline,
  PersonOutline,
  PeopleOutline,
  KeyOutline,
  ShieldOutline,
  DocumentOutline,
  FolderOutline,
  FolderOpenOutline,
  GridOutline,
  AppsOutline,
  ListOutline,
  MenuOutline,
  SearchOutline,
  AddOutline,
  CreateOutline,
  TrashOutline,
  CloudUploadOutline,
  CloudDownloadOutline,
  ImageOutline,
  CameraOutline,
  VideocamOutline,
  MusicalNotesOutline,
  DocumentTextOutline,
  NewspaperOutline,
  BookOutline,
  BookmarkOutline,
  CalendarOutline,
  TimeOutline,
  AlarmOutline,
  NotificationsOutline,
  ChatbubbleOutline,
  ChatbubblesOutline,
  MailOutline,
  SendOutline,
  ShareOutline,
  LinkOutline,
  GlobeOutline,
  MapOutline,
  LocationOutline,
  CompassOutline,
  NavigateOutline,
  CarOutline,
  AirplaneOutline,
  BoatOutline,
  BicycleOutline,
  WalkOutline,
  FitnessOutline,
  HeartOutline,
  StarOutline,
  TrophyOutline,
  FlagOutline,
  PricetagOutline,
  CartOutline,
  BagOutline,
  WalletOutline,
  CardOutline,
  CashOutline,
  StatsChartOutline,
  BarChartOutline,
  PieChartOutline,
  TrendingUpOutline,
  TrendingDownOutline,
  PulseOutline,
  AnalyticsOutline,
  SpeedometerOutline,
  ServerOutline,
  HardwareChipOutline,
  DesktopOutline,
  LaptopOutline,
  PhonePortraitOutline,
  TabletPortraitOutline,
  WatchOutline,
  TvOutline,
  PrintOutline,
  ScanOutline,
  QrCodeOutline,
  BarcodeOutline,
  WifiOutline,
  BluetoothOutline,
  CloudOutline,
  SunnyOutline,
  MoonOutline,
  ThunderstormOutline,
  RainyOutline,
  SnowOutline,
  ColorPaletteOutline,
  BrushOutline,
  BuildOutline,
  HammerOutline,
  ConstructOutline,
  CogOutline,
  OptionsOutline,
  ToggleOutline,
  LockClosedOutline,
  LockOpenOutline,
  EyeOutline,
  EyeOffOutline,
  HelpOutline,
  HelpCircleOutline,
  InformationCircleOutline,
  AlertCircleOutline,
  WarningOutline,
  CheckmarkCircleOutline,
  CloseCircleOutline,
  AddCircleOutline,
  RemoveCircleOutline,
  RefreshOutline,
  ReloadOutline,
  SyncOutline,
  DownloadOutline,
  LogInOutline,
  LogOutOutline,
  EnterOutline,
  ExitOutline,
  ExpandOutline,
  ContractOutline,
  ChevronUpOutline,
  ChevronDownOutline,
  ChevronBackOutline,
  ChevronForwardOutline,
  ArrowUpOutline,
  ArrowDownOutline,
  ArrowBackOutline,
  ArrowForwardOutline,
  SwapHorizontalOutline,
  SwapVerticalOutline,
  CopyOutline,
  ClipboardOutline,
  CutOutline,
  SaveOutline,
  CodeOutline,
  CodeSlashOutline,
  TerminalOutline,
  GitBranchOutline,
  GitCommitOutline,
  GitMergeOutline,
  GitPullRequestOutline,
  BugOutline,
  FlaskOutline,
  RocketOutline,
  FlashOutline,
  BulbOutline,
  ExtensionPuzzleOutline,
  LayersOutline,
  CubeOutline,
  ShapesOutline,
  EllipseOutline,
  SquareOutline,
  TriangleOutline,
  BusinessOutline,
  StorefrontOutline,
  SchoolOutline,
  LibraryOutline,
  MedkitOutline,
  RestaurantOutline,
  CafeOutline,
  BeerOutline,
  WineOutline,
  PizzaOutline,
  FastFoodOutline,
  IceCreamOutline,
  LeafOutline,
  FlowerOutline,
  PawOutline,
  FingerPrintOutline,
  HandLeftOutline,
  HandRightOutline,
  ThumbsUpOutline,
  ThumbsDownOutline,
  HappyOutline,
  SadOutline,
  AccessibilityOutline,
  BodyOutline,
  ManOutline,
  WomanOutline,
  MaleOutline,
  FemaleOutline,
  TransgenderOutline,
  PlanetOutline,
  EarthOutline,
  CloseOutline,
  EllipsisHorizontalOutline,
  EllipsisVerticalOutline,
  ReorderFourOutline,
  ReorderThreeOutline,
  ReorderTwoOutline
} from '@vicons/ionicons5'

defineOptions({ name: 'IconSelect' })

const props = defineProps<{
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const searchText = ref('')

// 图标列表
const iconList: { name: string; label: string; component: Component }[] = [
  { name: 'HomeOutline', label: '首页', component: markRaw(HomeOutline) },
  { name: 'SettingsOutline', label: '设置', component: markRaw(SettingsOutline) },
  { name: 'PersonOutline', label: '用户', component: markRaw(PersonOutline) },
  { name: 'PeopleOutline', label: '用户组', component: markRaw(PeopleOutline) },
  { name: 'KeyOutline', label: '密钥', component: markRaw(KeyOutline) },
  { name: 'ShieldOutline', label: '安全', component: markRaw(ShieldOutline) },
  { name: 'DocumentOutline', label: '文档', component: markRaw(DocumentOutline) },
  { name: 'FolderOutline', label: '文件夹', component: markRaw(FolderOutline) },
  { name: 'FolderOpenOutline', label: '打开文件夹', component: markRaw(FolderOpenOutline) },
  { name: 'GridOutline', label: '网格', component: markRaw(GridOutline) },
  { name: 'AppsOutline', label: '应用', component: markRaw(AppsOutline) },
  { name: 'ListOutline', label: '列表', component: markRaw(ListOutline) },
  { name: 'MenuOutline', label: '菜单', component: markRaw(MenuOutline) },
  { name: 'SearchOutline', label: '搜索', component: markRaw(SearchOutline) },
  { name: 'AddOutline', label: '添加', component: markRaw(AddOutline) },
  { name: 'CreateOutline', label: '创建', component: markRaw(CreateOutline) },
  { name: 'TrashOutline', label: '删除', component: markRaw(TrashOutline) },
  { name: 'CloudUploadOutline', label: '上传', component: markRaw(CloudUploadOutline) },
  { name: 'CloudDownloadOutline', label: '下载', component: markRaw(CloudDownloadOutline) },
  { name: 'ImageOutline', label: '图片', component: markRaw(ImageOutline) },
  { name: 'CameraOutline', label: '相机', component: markRaw(CameraOutline) },
  { name: 'VideocamOutline', label: '视频', component: markRaw(VideocamOutline) },
  { name: 'MusicalNotesOutline', label: '音乐', component: markRaw(MusicalNotesOutline) },
  { name: 'DocumentTextOutline', label: '文档文本', component: markRaw(DocumentTextOutline) },
  { name: 'NewspaperOutline', label: '新闻', component: markRaw(NewspaperOutline) },
  { name: 'BookOutline', label: '书籍', component: markRaw(BookOutline) },
  { name: 'BookmarkOutline', label: '书签', component: markRaw(BookmarkOutline) },
  { name: 'CalendarOutline', label: '日历', component: markRaw(CalendarOutline) },
  { name: 'TimeOutline', label: '时间', component: markRaw(TimeOutline) },
  { name: 'AlarmOutline', label: '闹钟', component: markRaw(AlarmOutline) },
  { name: 'NotificationsOutline', label: '通知', component: markRaw(NotificationsOutline) },
  { name: 'ChatbubbleOutline', label: '聊天', component: markRaw(ChatbubbleOutline) },
  { name: 'ChatbubblesOutline', label: '聊天组', component: markRaw(ChatbubblesOutline) },
  { name: 'MailOutline', label: '邮件', component: markRaw(MailOutline) },
  { name: 'SendOutline', label: '发送', component: markRaw(SendOutline) },
  { name: 'ShareOutline', label: '分享', component: markRaw(ShareOutline) },
  { name: 'LinkOutline', label: '链接', component: markRaw(LinkOutline) },
  { name: 'GlobeOutline', label: '全球', component: markRaw(GlobeOutline) },
  { name: 'MapOutline', label: '地图', component: markRaw(MapOutline) },
  { name: 'LocationOutline', label: '位置', component: markRaw(LocationOutline) },
  { name: 'CompassOutline', label: '指南针', component: markRaw(CompassOutline) },
  { name: 'NavigateOutline', label: '导航', component: markRaw(NavigateOutline) },
  { name: 'CarOutline', label: '汽车', component: markRaw(CarOutline) },
  { name: 'AirplaneOutline', label: '飞机', component: markRaw(AirplaneOutline) },
  { name: 'BoatOutline', label: '船', component: markRaw(BoatOutline) },
  { name: 'BicycleOutline', label: '自行车', component: markRaw(BicycleOutline) },
  { name: 'WalkOutline', label: '步行', component: markRaw(WalkOutline) },
  { name: 'FitnessOutline', label: '健身', component: markRaw(FitnessOutline) },
  { name: 'HeartOutline', label: '心形', component: markRaw(HeartOutline) },
  { name: 'StarOutline', label: '星星', component: markRaw(StarOutline) },
  { name: 'TrophyOutline', label: '奖杯', component: markRaw(TrophyOutline) },
  { name: 'FlagOutline', label: '旗帜', component: markRaw(FlagOutline) },
  { name: 'PricetagOutline', label: '标签', component: markRaw(PricetagOutline) },
  { name: 'CartOutline', label: '购物车', component: markRaw(CartOutline) },
  { name: 'BagOutline', label: '购物袋', component: markRaw(BagOutline) },
  { name: 'WalletOutline', label: '钱包', component: markRaw(WalletOutline) },
  { name: 'CardOutline', label: '卡片', component: markRaw(CardOutline) },
  { name: 'CashOutline', label: '现金', component: markRaw(CashOutline) },
  { name: 'StatsChartOutline', label: '统计图', component: markRaw(StatsChartOutline) },
  { name: 'BarChartOutline', label: '柱状图', component: markRaw(BarChartOutline) },
  { name: 'PieChartOutline', label: '饼图', component: markRaw(PieChartOutline) },
  { name: 'TrendingUpOutline', label: '上升', component: markRaw(TrendingUpOutline) },
  { name: 'TrendingDownOutline', label: '下降', component: markRaw(TrendingDownOutline) },
  { name: 'PulseOutline', label: '脉冲', component: markRaw(PulseOutline) },
  { name: 'AnalyticsOutline', label: '分析', component: markRaw(AnalyticsOutline) },
  { name: 'SpeedometerOutline', label: '仪表盘', component: markRaw(SpeedometerOutline) },
  { name: 'ServerOutline', label: '服务器', component: markRaw(ServerOutline) },
  { name: 'HardwareChipOutline', label: '芯片', component: markRaw(HardwareChipOutline) },
  { name: 'DesktopOutline', label: '桌面', component: markRaw(DesktopOutline) },
  { name: 'LaptopOutline', label: '笔记本', component: markRaw(LaptopOutline) },
  { name: 'PhonePortraitOutline', label: '手机', component: markRaw(PhonePortraitOutline) },
  { name: 'TabletPortraitOutline', label: '平板', component: markRaw(TabletPortraitOutline) },
  { name: 'WatchOutline', label: '手表', component: markRaw(WatchOutline) },
  { name: 'TvOutline', label: '电视', component: markRaw(TvOutline) },
  { name: 'PrintOutline', label: '打印', component: markRaw(PrintOutline) },
  { name: 'ScanOutline', label: '扫描', component: markRaw(ScanOutline) },
  { name: 'QrCodeOutline', label: '二维码', component: markRaw(QrCodeOutline) },
  { name: 'BarcodeOutline', label: '条码', component: markRaw(BarcodeOutline) },
  { name: 'WifiOutline', label: 'WiFi', component: markRaw(WifiOutline) },
  { name: 'BluetoothOutline', label: '蓝牙', component: markRaw(BluetoothOutline) },
  { name: 'CloudOutline', label: '云', component: markRaw(CloudOutline) },
  { name: 'SunnyOutline', label: '晴天', component: markRaw(SunnyOutline) },
  { name: 'MoonOutline', label: '月亮', component: markRaw(MoonOutline) },
  { name: 'ThunderstormOutline', label: '雷暴', component: markRaw(ThunderstormOutline) },
  { name: 'RainyOutline', label: '下雨', component: markRaw(RainyOutline) },
  { name: 'SnowOutline', label: '下雪', component: markRaw(SnowOutline) },
  { name: 'ColorPaletteOutline', label: '调色板', component: markRaw(ColorPaletteOutline) },
  { name: 'BrushOutline', label: '画笔', component: markRaw(BrushOutline) },
  { name: 'BuildOutline', label: '构建', component: markRaw(BuildOutline) },
  { name: 'HammerOutline', label: '锤子', component: markRaw(HammerOutline) },
  { name: 'ConstructOutline', label: '施工', component: markRaw(ConstructOutline) },
  { name: 'CogOutline', label: '齿轮', component: markRaw(CogOutline) },
  { name: 'OptionsOutline', label: '选项', component: markRaw(OptionsOutline) },
  { name: 'ToggleOutline', label: '开关', component: markRaw(ToggleOutline) },
  { name: 'LockClosedOutline', label: '锁定', component: markRaw(LockClosedOutline) },
  { name: 'LockOpenOutline', label: '解锁', component: markRaw(LockOpenOutline) },
  { name: 'EyeOutline', label: '查看', component: markRaw(EyeOutline) },
  { name: 'EyeOffOutline', label: '隐藏', component: markRaw(EyeOffOutline) },
  { name: 'HelpOutline', label: '帮助', component: markRaw(HelpOutline) },
  { name: 'HelpCircleOutline', label: '帮助圆', component: markRaw(HelpCircleOutline) },
  { name: 'InformationCircleOutline', label: '信息', component: markRaw(InformationCircleOutline) },
  { name: 'AlertCircleOutline', label: '警告', component: markRaw(AlertCircleOutline) },
  { name: 'WarningOutline', label: '警告三角', component: markRaw(WarningOutline) },
  { name: 'CheckmarkCircleOutline', label: '成功', component: markRaw(CheckmarkCircleOutline) },
  { name: 'CloseCircleOutline', label: '关闭', component: markRaw(CloseCircleOutline) },
  { name: 'AddCircleOutline', label: '添加圆', component: markRaw(AddCircleOutline) },
  { name: 'RemoveCircleOutline', label: '移除圆', component: markRaw(RemoveCircleOutline) },
  { name: 'RefreshOutline', label: '刷新', component: markRaw(RefreshOutline) },
  { name: 'ReloadOutline', label: '重新加载', component: markRaw(ReloadOutline) },
  { name: 'SyncOutline', label: '同步', component: markRaw(SyncOutline) },
  { name: 'DownloadOutline', label: '下载', component: markRaw(DownloadOutline) },
  { name: 'LogInOutline', label: '登录', component: markRaw(LogInOutline) },
  { name: 'LogOutOutline', label: '登出', component: markRaw(LogOutOutline) },
  { name: 'EnterOutline', label: '进入', component: markRaw(EnterOutline) },
  { name: 'ExitOutline', label: '退出', component: markRaw(ExitOutline) },
  { name: 'ExpandOutline', label: '展开', component: markRaw(ExpandOutline) },
  { name: 'ContractOutline', label: '收缩', component: markRaw(ContractOutline) },
  { name: 'CopyOutline', label: '复制', component: markRaw(CopyOutline) },
  { name: 'ClipboardOutline', label: '剪贴板', component: markRaw(ClipboardOutline) },
  { name: 'CutOutline', label: '剪切', component: markRaw(CutOutline) },
  { name: 'SaveOutline', label: '保存', component: markRaw(SaveOutline) },
  { name: 'CodeOutline', label: '代码', component: markRaw(CodeOutline) },
  { name: 'CodeSlashOutline', label: '代码斜杠', component: markRaw(CodeSlashOutline) },
  { name: 'TerminalOutline', label: '终端', component: markRaw(TerminalOutline) },
  { name: 'GitBranchOutline', label: 'Git分支', component: markRaw(GitBranchOutline) },
  { name: 'GitCommitOutline', label: 'Git提交', component: markRaw(GitCommitOutline) },
  { name: 'GitMergeOutline', label: 'Git合并', component: markRaw(GitMergeOutline) },
  { name: 'GitPullRequestOutline', label: 'Git PR', component: markRaw(GitPullRequestOutline) },
  { name: 'BugOutline', label: 'Bug', component: markRaw(BugOutline) },
  { name: 'FlaskOutline', label: '实验', component: markRaw(FlaskOutline) },
  { name: 'RocketOutline', label: '火箭', component: markRaw(RocketOutline) },
  { name: 'FlashOutline', label: '闪电', component: markRaw(FlashOutline) },
  { name: 'BulbOutline', label: '灯泡', component: markRaw(BulbOutline) },
  { name: 'ExtensionPuzzleOutline', label: '扩展', component: markRaw(ExtensionPuzzleOutline) },
  { name: 'LayersOutline', label: '图层', component: markRaw(LayersOutline) },
  { name: 'CubeOutline', label: '立方体', component: markRaw(CubeOutline) },
  { name: 'ShapesOutline', label: '形状', component: markRaw(ShapesOutline) },
  { name: 'EllipseOutline', label: '椭圆', component: markRaw(EllipseOutline) },
  { name: 'SquareOutline', label: '方形', component: markRaw(SquareOutline) },
  { name: 'TriangleOutline', label: '三角形', component: markRaw(TriangleOutline) },
  { name: 'BusinessOutline', label: '商业', component: markRaw(BusinessOutline) },
  { name: 'StorefrontOutline', label: '店铺', component: markRaw(StorefrontOutline) },
  { name: 'SchoolOutline', label: '学校', component: markRaw(SchoolOutline) },
  { name: 'LibraryOutline', label: '图书馆', component: markRaw(LibraryOutline) },
  { name: 'MedkitOutline', label: '医疗', component: markRaw(MedkitOutline) },
  { name: 'RestaurantOutline', label: '餐厅', component: markRaw(RestaurantOutline) },
  { name: 'CafeOutline', label: '咖啡', component: markRaw(CafeOutline) },
  { name: 'LeafOutline', label: '树叶', component: markRaw(LeafOutline) },
  { name: 'FlowerOutline', label: '花朵', component: markRaw(FlowerOutline) },
  { name: 'PawOutline', label: '脚印', component: markRaw(PawOutline) },
  { name: 'FingerPrintOutline', label: '指纹', component: markRaw(FingerPrintOutline) },
  { name: 'ThumbsUpOutline', label: '点赞', component: markRaw(ThumbsUpOutline) },
  { name: 'ThumbsDownOutline', label: '踩', component: markRaw(ThumbsDownOutline) },
  { name: 'HappyOutline', label: '开心', component: markRaw(HappyOutline) },
  { name: 'SadOutline', label: '难过', component: markRaw(SadOutline) },
  { name: 'AccessibilityOutline', label: '辅助功能', component: markRaw(AccessibilityOutline) },
  { name: 'PlanetOutline', label: '星球', component: markRaw(PlanetOutline) },
  { name: 'EarthOutline', label: '地球', component: markRaw(EarthOutline) },
  { name: 'EllipsisHorizontalOutline', label: '更多(横)', component: markRaw(EllipsisHorizontalOutline) },
  { name: 'EllipsisVerticalOutline', label: '更多(竖)', component: markRaw(EllipsisVerticalOutline) },
  { name: 'ReorderFourOutline', label: '排序4', component: markRaw(ReorderFourOutline) },
  { name: 'ReorderThreeOutline', label: '排序3', component: markRaw(ReorderThreeOutline) },
  { name: 'ReorderTwoOutline', label: '排序2', component: markRaw(ReorderTwoOutline) }
]

// 过滤图标
const filteredIcons = computed(() => {
  if (!searchText.value) return iconList
  const keyword = searchText.value.toLowerCase()
  return iconList.filter(
    icon => icon.name.toLowerCase().includes(keyword) || icon.label.includes(keyword)
  )
})

// 图标映射
const iconMap: Record<string, Component> = {}
iconList.forEach(icon => {
  iconMap[icon.name] = icon.component
})

// 获取图标组件
function getIconComponent(name: string): Component | undefined {
  return iconMap[name]
}

// 选择图标
function handleSelect(name: string) {
  emit('update:modelValue', name)
}

// 清除
function handleClear() {
  emit('update:modelValue', '')
}

// 导出获取图标方法供外部使用
defineExpose({ getIconComponent, iconMap })
</script>

<style scoped lang="scss">
.icon-select-container {
  padding: 4px;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 8px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;

  &:hover {
    background-color: #f0f0f0;
  }

  &.active {
    background-color: #e8f4ff;
    border-color: #1890ff;
    color: #1890ff;
  }

  .icon-name {
    font-size: 10px;
    margin-top: 4px;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    max-width: 100%;
  }
}
</style>
