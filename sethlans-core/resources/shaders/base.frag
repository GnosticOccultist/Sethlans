#version 450

layout(location = 0) in vec2 fragUV;

layout(location = 0) out vec4 fragColor;

layout(set = 2, binding = 0) uniform sampler2D textureSampler;

void main() {
	fragColor = texture(textureSampler, fragUV);
}