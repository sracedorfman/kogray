#version 300 es 
precision highp float;

uniform struct {
  mat4 rayDirMatrix;
  vec3 position;
} camera;

in vec2 tex;
in vec4 rayDir;

uniform struct {
  samplerCube envTexture;
  float freq;
  float noiseFreq;
  float noiseExp;
  float noiseAmp;
} material;

uniform struct {
	mat4 surface;
	mat4 clipper0;
	float invClipper0;
	mat4 clipper1;
	float invClipper1;
	vec3 kd;
	vec3 kr;
	float lace;
} quadrics[15];

out vec4 fragmentColor; // Chessboard int(hit.x)%2 == int(hit.y)%2

float snoise(vec3 r) {
  vec3 s = vec3(7502, 22777, 4767);
  float f = 0.0;
  for(int i=0; i<16; i++) {
    f += sin( dot(s - vec3(32768, 32768, 32768), r)
                                 / 65536.0);
    s = mod(s, 32768.0) * 2.0 + floor(s / 32768.0);
  }
  return f / 32.0 + 0.5;
}

float intersectQuadric(
	vec4 e, vec4 d,
	mat4 A, mat4 B,
	mat4 C, bool BInv,
	bool CInv, bool lace){
	float a = dot(d * A, d);
	float b = dot(d * A, e) + dot(e * A, d);
	float c = dot(e * A, e);

	float det = b*b - 4.0 * a * c;
	if(det < 0.0)
		return -1.0;
	if (a != 0.0) {
		float t1 = (-b + sqrt(det)) / (2.0 * a);	
		float t2 = (-b - sqrt(det)) / (2.0 * a);
		vec4 hit1 = e + d * t1;
		vec4 hit2 = e + d * t2;
		if (BInv) {
			if( dot(hit1 * B, hit1) < 0.0 ){
				t1 = -1.0;
			}
			if( dot(hit2 * B, hit2) < 0.0 ){
				t2 = -1.0;
			}
		} else {
			if( dot(hit1 * B, hit1) > 0.0 ){
				t1 = -1.0;
			}
			if( dot(hit2 * B, hit2) > 0.0 ){
				t2 = -1.0;
			}
		}
		if (CInv) {
			if( dot(hit1 * C, hit1) < 0.0 ){
				t1 = -1.0;
			}
			if( dot(hit2 * C, hit2) < 0.0 ){
				t2 = -1.0;
			}
		} else {
			if( dot(hit1 * C, hit1) > 0.0 ){
				t1 = -1.0;
			}
			if( dot(hit2 * C, hit2) > 0.0 ){
				t2 = -1.0;
			}
		}
		if (lace) {
			float w1x = fract(hit1.x * material.freq
				+ pow(
					snoise(hit1.xyz * material.noiseFreq),
					material.noiseExp)
					* material.noiseAmp
				);
			float w1z = fract(hit1.z * material.freq
				+ pow(
					snoise(hit1.xyz * material.noiseFreq),
					material.noiseExp)
					* material.noiseAmp
				);
			if (w1x + w1z > 1.0)
				t1 = -1.0;
			float w2x = fract(hit2.x * material.freq
				+ pow(
					snoise(hit2.xyz * material.noiseFreq),
					material.noiseExp)
					* material.noiseAmp
				);
			float w2z = fract(hit2.z * material.freq
				+ pow(
					snoise(hit2.xyz * material.noiseFreq),
					material.noiseExp)
					* material.noiseAmp
				);
			if (w2x + w2z > 1.0)
				t2 = -1.0;
		}
		return (t1<0.0)?t2: (t2<0.0)? t1 : min(t1, t2);
	} else {
		float t = -c/b;
		vec4 hit1 = e + d * t;
		if( dot(hit1 * B, hit1) > 0.0 ){
			t = -1.0;
		}
		if( dot(hit1 * C, hit1) > 0.0 ){
			t = -1.0;
		}
		return t;
	}
}

bool findBestHit(vec4 e, vec4 d, out float bestT, out int bestIndex){
	bestT = 9001.0;
	for(int i=0; i<15; i++){
		float t = intersectQuadric(e, d, 
  		quadrics[i].surface, quadrics[i].clipper0, quadrics[i].clipper1,
		  quadrics[i].invClipper0 == 1.0, quadrics[i].invClipper1 == 1.0,
		  quadrics[i].lace == 1.0);
		if(t < bestT && t > 0.0){
			bestT = t;
			bestIndex = i;
		}
	}
	if(bestT > 9000.0)
		return false;
	return true; 
}

void main(void) {
	vec4 d = vec4(normalize(rayDir.xyz), 0);
	vec4 e = vec4(camera.position, 1);

	vec3 radiance = vec3(0, 0, 0);
	vec3 accumulatedScatteringProb = vec3(1,1,1);

	for (int i = 0; i < 6; i++) {
		int index;
		float t;
		if( findBestHit(e, d, t, index) ){
			vec4 hit = e + d * t;
			vec3 normal = normalize((hit * quadrics[index].surface + quadrics[index].surface * hit).xyz); 

			if (dot(normal, d.xyz) > 0.0)
				normal = -normal;
			//shading here
			vec3 lightDir = normalize(vec3(1.0, 1.0, -.7));

			vec4 shadowRayE = hit;
			shadowRayE.xyz += 0.01 * normal;
			vec4 shadowRayD = vec4(lightDir, 0.0);
			int shadowCasterIndex;
			float shadowCasterT;
			bool shadowCasterIntersected = findBestHit(shadowRayE, shadowRayD, shadowCasterT, shadowCasterIndex);
			if (!shadowCasterIntersected) {
				if (index == 0) {
					vec3 color;
					if (int(abs(hit.x-4.0))%2 == int(abs(hit.z-4.0))%2)
						color = vec3(1,1,1);
					else
						color = vec3(0,0,0);
					radiance += color;
				} else {
					radiance += (quadrics[index].kd.rgb * accumulatedScatteringProb)
						* clamp(dot(lightDir, normal), 0.0, 1.0);
				}
			}

			if (index != 0) {
				e = hit;
				e.xyz += normal * 0.01;
				d.xyz = reflect(d.xyz, normal);
				accumulatedScatteringProb *= quadrics[index].kr;
			} else {
				break;
			}

			// radiance += quadrics[index].kd.rgb;

			// radiance += normal;
		} else {
			radiance += texture(material.envTexture, d.xyz).rgb;
			break;
		}
		
		
	}
  	fragmentColor = vec4(radiance, 1);
}