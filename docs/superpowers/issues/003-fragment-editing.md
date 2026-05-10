# Issue 003: 碎片记录后无法编辑，需支持修改已保存的碎片

## 背景
用户添加碎片后，如果发现错别字、想补充内容或调整措辞，当前没有任何入口可以修改已保存的碎片。只能删除后重新录入，体验不佳。

## 现状分析
- `CaptureScreen.kt` 目前只支持**新增**碎片（`viewModel.addFragment(content)`）
- `HomeScreen.kt` 的"最近记录"区域仅以只读 Card 展示碎片内容，无点击编辑交互
- `DiaryViewModel` 里有 `deleteFragment`，但没有 `updateFragmentContent` 或类似方法
- `DiaryRepository` 有 `updateFragment(fragment: Fragment)`，但 ViewModel 没有对外暴露内容修改的便捷方法

## 期望功能
1. **点击编辑**：在 HomeScreen 的碎片列表中，点击任意碎片卡片，进入编辑模式（可复用 CaptureScreen 或新建 EditScreen）
2. **编辑页能力**：
   - 预填充原有内容
   - 支持修改后保存（`repository.updateFragment`）
   - 支持取消返回
3. **编辑后状态处理**：
   - 若碎片之前已进入过 pipeline（`pipelineStep != IDLE`），编辑后建议将其状态重置为 `IDLE` 或 `PREPROCESSED`，以便重新生成日记时能纳入该碎片
   - 或至少给出提示："编辑后重新生成日记将使用更新后的内容"
4. **日记列表/详情中的碎片关联展示**：在 `DiaryDetailScreen` 中展示与该日记关联的碎片时，也提供跳转编辑的入口

## 影响范围
- `ui/screens/HomeScreen.kt` — 碎片卡片增加点击事件
- `ui/screens/CaptureScreen.kt` 或新建 `EditFragmentScreen.kt` — 支持编辑模式
- `ui/viewmodel/DiaryViewModel.kt` — 新增 `editFragment(fragmentId, newContent)` 方法
- `data/repository/DiaryRepository.kt` — 已有 `updateFragment`，逻辑上可直接复用

## 验收标准
- [x] 首页碎片列表点击后可进入编辑页
- [x] 编辑页能正确回显原内容，保存后即时刷新列表
- [x] 编辑已参与过日记生成的碎片后，pipeline 状态重置为 IDLE
- [x] 所有现有单元测试通过
