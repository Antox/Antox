/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#pragma version(1)
#pragma rs java_package_name(chat.tox.antox)
#pragma rs_fp_relaxed

uint y_stride;
uint u_stride;
uint v_stride;
rs_allocation y_data;
rs_allocation u_data;
rs_allocation v_data;

static int CLIP(int X) { return ((X) > 255 ? 255 : (X) < 0 ? 0 : X); }

// RGB -> YUV
static int RGB2Y(int R, int G, int B) { return CLIP((( 66 * (R) + 129 * (G) +  25 * (B) + 128) >> 8) +  16); }
static int RGB2U(int R ,int G, int B) { return CLIP(((-38 * (R) -  74 * (G) + 112 * (B) + 128) >> 8) + 128); }
static int RGB2V(int R, int G, int B) { return CLIP(((112 * (R) -  94 * (G) -  18 * (B) + 128) >> 8) + 128); }

// YUV -> RGB
static int C(int Y) { return ((Y) - 16  ); }
static int D(int U) { return ((U) - 128  ); }
static int E(int V) { return ((V) - 128  ); }

static int YUV2R(int Y, int U, int V) { return CLIP((298 * C(Y)              + 409 * E(V) + 128) >> 8); }
static int YUV2G(int Y, int U, int V) { return CLIP((298 * C(Y) - 100 * D(U) - 208 * E(V) + 128) >> 8); }
static int YUV2B(int Y, int U, int V) { return CLIP((298 * C(Y) + 516 * D(U)              + 128) >> 8); }

uchar4 __attribute__((kernel)) yuvToRgb(uchar ignored, uint32_t x, uint32_t y) {

    uchar yx = rsGetElementAt_uchar(y_data, (y * y_stride) + x);
    uchar ux = rsGetElementAt_uchar(u_data, ((y / 2) * u_stride) + (x / 2));
    uchar vx = rsGetElementAt_uchar(v_data, ((y / 2) * v_stride) + (x / 2));

    int4 rgb;
    rgb.r = YUV2R(yx, ux, vx);
    rgb.g = YUV2G(yx, ux, vx);
    rgb.b = YUV2B(yx, ux, vx);
    rgb.a = 255;

    uchar4 out = convert_uchar4(rgb);

    return out;
}