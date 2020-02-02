/*
 * Copyright (c) 2020. Morgan Lewis <https://github.com/MESLewis>
 * This file is part of the SimpleGLTF2 project which is released under the MIT License.
 * See <https://github.com/MESLewis/SimpleGLTF2/blob/master/LICENSE> for more information
 */

package com.meslewis.simplegltf2.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * The material appearance of a primitive.
 */
public class GLTFMaterial extends GLTFChildOfRootProperty {

  /**
   * A set of parameter values that are used to define the metallic-roughness material model from
   * Physically-Based Rendering (PBR) methodology. When not specified, all the default values of
   * `pbrMetallicRoughness` apply.
   */
  @JsonProperty("pbrMetallicRoughness")
  private GLTFPBRMetallicRoughness pbrMetallicRoughness;

  /**
   * A tangent space normal map. The texture contains RGB components in linear space. Each texel
   * represents the XYZ components of a normal vector in tangent space. Red [0 to 255] maps to X [-1
   * to 1]. Green [0 to 255] maps to Y [-1 to 1]. Blue [128 to 255] maps to Z [1/255 to 1]. The
   * normal vectors use OpenGL conventions where +X is right and +Y is up. +Z points toward the
   * viewer. In GLSL, this vector would be unpacked like so: `float3 normalVector = tex2D(<sampled
   * normal map texture value>, texCoord) * 2 - 1`. Client implementations should normalize the
   * normal vectors before using them in lighting equations.
   */
  @JsonProperty("normalTexture")
  private GLTFTextureInfo.GLTFNormalTextureInfo normalTexture;

  /**
   * The occlusion map texture. The occlusion values are sampled from the R channel. Higher values
   * indicate areas that should receive full indirect lighting and lower values indicate no indirect
   * lighting. These values are linear. If other channels are present (GBA), they are ignored for
   * occlusion calculations.
   */
  @JsonProperty("occlusionTexture")
  private GLTFTextureInfo.GLTFOcclusionTextureInfo occlusionTexture;

  /**
   * The emissive map controls the color and intensity of the light being emitted by the material.
   * This texture contains RGB components encoded with the sRGB transfer function. If a fourth
   * component (A) is present, it is ignored.
   */
  @JsonProperty("emissiveTexture")
  private GLTFTextureInfo emissiveTexture;

  /**
   * The RGB components of the emissive color of the material. These values are linear. If an
   * emissiveTexture is specified, this value is multiplied with the texel values.
   */
  @JsonProperty("emissiveFactor")
  private float[] emissiveFactor = {0.0f, 0.0f, 0.0f};

  /**
   * The material's alpha rendering mode enumeration specifying the interpretation of the alpha
   * value of the main factor and texture.
   */
  @JsonProperty("alphaMode")
  @NotNull
  private GLTFAlphaMode alphaMode = GLTFAlphaMode.OPAQUE;

  /**
   * Specifies the cutoff threshold when in `MASK` mode. If the alpha value is greater than or equal
   * to this value then it is rendered as fully opaque, otherwise, it is rendered as fully
   * transparent. A value greater than 1.0 will render the entire material as fully transparent.
   * This value is ignored for other modes.
   */
  @JsonProperty("alphaCutoff")
  @Min(0)
  private float alphaCutoff = 0.5f;

  /**
   * Specifies whether the material is double sided. When this value is false, back-face culling is
   * enabled. When this value is true, back-face culling is disabled and double sided lighting is
   * enabled. The back-face must have its normals reversed before the lighting equation is
   * evaluated.
   */
  @JsonProperty("doubleSided")
  private boolean doubleSided = false;

  public GLTFPBRMetallicRoughness getPbrMetallicRoughness() {
    return pbrMetallicRoughness;
  }

  public GLTFTextureInfo.GLTFNormalTextureInfo getNormalTexture() {
    return normalTexture;
  }

  public GLTFTextureInfo.GLTFOcclusionTextureInfo getOcclusionTexture() {
    return occlusionTexture;
  }

  public GLTFTextureInfo getEmissiveTexture() {
    return emissiveTexture;
  }

  public float[] getEmissiveFactor() {
    return emissiveFactor;
  }

  public GLTFAlphaMode getAlphaMode() {
    return alphaMode;
  }

  public float getAlphaCutoff() {
    return alphaCutoff;
  }

  public boolean isDoubleSided() {
    return doubleSided;
  }

  @Override
  public String toString() {
    return "GLTFMaterial{" +
        "pbrMetallicRoughness=" + pbrMetallicRoughness +
        ", normalTexture=" + normalTexture +
        ", occlusionTexture=" + occlusionTexture +
        ", emissiveTexture=" + emissiveTexture +
        ", emissiveFactor=" + Arrays.toString(emissiveFactor) +
        ", alphaMode=" + alphaMode +
        ", alphaCutoff=" + alphaCutoff +
        ", doubleSided=" + doubleSided +
        '}';
  }
}
