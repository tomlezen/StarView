# 简单的星星控件
<img src="https://github.com/tomlezen/StarView/blob/master/screenshot/ezgif.com-video-to-gif.gif?raw=true" alt="arc" style="max-width:100%;">

## Gradle

```
implementation 'com.github.tomlezen:StarView:1.0.0'
```
## 使用

```
<!-- 是否可以选择. -->
<attr name="star_selectable" format="boolean" />
<!-- 星星总数. -->
<attr name="star_total_num" format="integer" />
<!-- 当前星星数. -->
<attr name="star_num" format="float" />
<!-- 星星大小. -->
<attr name="star_size" format="dimension" />
<!-- 星星半径比例. -->
<attr name="star_radius_scale" format="float" />
<!-- 星星之间间隔. -->
<attr name="star_space" format="dimension" />
<!-- 星星选中颜色. -->
<attr name="star_select_color" format="color" />
<!-- 星星颜色. -->
<attr name="star_color" format="color" />
<!-- 星星线条宽度. -->
<attr name="star_stroke_width" format="color" />

// 选择回调.
StarView.onStarSelected = {
}
```
