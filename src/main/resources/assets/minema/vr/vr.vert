#version 120

varying vec4 texcoord;

void main()
{
    gl_Position = gl_Vertex;
    texcoord = gl_MultiTexCoord0;
}