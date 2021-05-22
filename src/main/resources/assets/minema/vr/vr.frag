#version 120

uniform samplerCube tex;

varying vec4 texcoord;

void main()
{
    vec2 rad = radians(vec2(180, 90)) * (texcoord.xy * 2.0 - 1.0);
    vec3 dir = vec3(-cos(rad.y) * sin(rad.x), -sin(rad.y), -cos(rad.y) * cos(rad.x));
    gl_FragColor = textureCube(tex, dir);
}