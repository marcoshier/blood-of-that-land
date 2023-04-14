package tools

import org.openrndr.color.ColorRGBa
import org.openrndr.draw.Filter1to1
import org.openrndr.draw.filterShaderFromCode
import org.openrndr.extra.color.presets.ORANGE
import org.openrndr.extra.parameters.ColorParameter
import org.openrndr.extra.parameters.Description

val colorRangeShader = """
       
          in vec2 v_texCoord0;
          uniform sampler2D tex0;
          uniform vec4 background;
          uniform vec4 foreground;
          out vec4 o_color;
        
          void main() {
              vec3 c = vec3(0.4);
              vec3 fill = texture(tex0, v_texCoord0).xyz;
              
              if(any(lessThan(fill, foreground.xyz))) {
                    c = fill.xyz;
              } else {
                    c = background.xyz;
              }
              
              o_color = vec4(c, 1.0);
          }
        
""".trimIndent()

@Description("ColorMoreThan")
class ColorMoreThan: Filter1to1(filterShaderFromCode(colorRangeShader, "color-range-shader")) {

    @ColorParameter("foreground")
    var foreground: ColorRGBa by parameters
    var background: ColorRGBa by parameters

    init {
        foreground = ColorRGBa.ORANGE
        background = ColorRGBa.BLACK
    }

}