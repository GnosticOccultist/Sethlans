#version 450

layout(location = 0) in vec2 fragUV;

layout(location = 0) out vec4 fragColor;

void main() {
	fragColor = vec4(fragUV.x, fragUV.y, 0, 1);
}