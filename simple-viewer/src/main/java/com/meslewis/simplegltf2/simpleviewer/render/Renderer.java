/*
 * Copyright (c) 2020. Morgan Lewis <https://github.com/MESLewis>
 * This file is part of the SimpleGLTF2 project which is released under the MIT License.
 * See <https://github.com/MESLewis/SimpleGLTF2/blob/master/LICENSE> for more information
 */

package com.meslewis.simplegltf2.simpleviewer.render;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_CULL_FACE;
import static org.lwjgl.opengl.GL11.GL_DOUBLE;
import static org.lwjgl.opengl.GL11.GL_ONE;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_SHORT;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL11.glDrawElements;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14.glBlendEquation;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glDisableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

import com.meslewis.simplegltf2.data.GLTFAccessor;
import com.meslewis.simplegltf2.data.GLTFAlphaMode;
import com.meslewis.simplegltf2.simpleviewer.GlUtil;
import com.meslewis.simplegltf2.simpleviewer.IOUtil;
import com.meslewis.simplegltf2.simpleviewer.ShaderCache;
import com.meslewis.simplegltf2.simpleviewer.ShaderDebugType;
import com.meslewis.simplegltf2.simpleviewer.ShaderProgram;
import com.meslewis.simplegltf2.simpleviewer.UniformLight;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Renderer {

  private static final Logger logger = LoggerFactory.getLogger(Renderer.class);

  private List<RenderLight> visibleLights;

  private Matrix4f projMatrix;
  private Matrix4f viewMatrix;
  private Matrix4f viewProjectionMatrix = new Matrix4f();
  private int nodeDrawLimit = -1;
  private boolean drawInvisibleNodes = false; //Draw all nodes on the scene tree

  private RenderCamera camera;
  private RenderEnvironmentMap envData;

  private final int debugBuf;
  private final int debugEle;

  private ShaderDebugType debugType = ShaderDebugType.NONE;

  //TODO global settings
  private boolean usePunctualLighting = false;
  private boolean useIBL = true;
  public static final boolean generateMipmaps = true;

  public Renderer() {
    visibleLights = new ArrayList<>();

    RenderLight light1 = new RenderLight(null, null);
//    RenderLight light2 = new RenderLight(null, null);

    //TODO set up a second default light
//    UniformLight ul2 = light2.getUniformLight();
//    ul2.position = ul2.position.add(-5, 2, -5);

    visibleLights.add(light1);
//    visibleLights.add(light2);

    //Setup debug box
    double[] debugBox = {
        -1.0, -1.0, 1.0,
        1.0, -1.0, 1.0,
        1.0, 1.0, 1.0,
        -1.0, 1.0, 1.0,
        -1.0, -1.0, -1.0,
        1.0, -1.0, -1.0,
        1.0, 1.0, -1.0,
        -1.0, 1.0, -1.0
    };
    debugBuf = glGenBuffers();
    glBindBuffer(GL_ARRAY_BUFFER, debugBuf);
    DoubleBuffer nBuffer = ByteBuffer.allocateDirect(debugBox.length * Double.BYTES).order(
        ByteOrder.nativeOrder()).asDoubleBuffer();
    nBuffer.put(debugBox);
    nBuffer.flip();
    glBufferData(GL_ARRAY_BUFFER, nBuffer, GL_STATIC_DRAW);
    glBindBuffer(GL_ARRAY_BUFFER, 0);

    short[] debugElements = {
        0, 1, 2,
        2, 3, 0,
        1, 5, 6,
        6, 2, 1,
        7, 6, 5,
        5, 4, 7,
        4, 0, 3,
        3, 7, 4,
        4, 5, 1,
        1, 0, 4,
        3, 2, 6,
        6, 7, 3
    };

    debugEle = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, debugEle);
    ShortBuffer nShortBuffer = ByteBuffer.allocateDirect(debugElements.length * Short.BYTES)
        .order(ByteOrder.nativeOrder()).asShortBuffer();
    nShortBuffer.put(debugElements);
    nShortBuffer.flip();
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, nShortBuffer, GL_STATIC_DRAW);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

    //Set up environment map
    envData = new RenderEnvironmentMap(
        IOUtil.getResource("environments/studio_grey/"));
  }

  public void draw(RenderCamera camera, RenderNode rootNode, int targetDrawLimit) {
    this.camera = camera;

    nodeDrawLimit = targetDrawLimit;
    List<RenderMeshPrimitive> transparentNodes = new ArrayList<>();
    draw(rootNode, transparentNodes);

    //TODO sort by distance
    for (RenderMeshPrimitive renderMeshPrimitive : transparentNodes) {
      drawRenderObject(renderMeshPrimitive);
    }
  }

  /**
   * Walk down the RenderNode tree
   *
   * @param node
   */
  private void draw(RenderNode node, List<RenderMeshPrimitive> transparentNodes) {
    if (node instanceof RenderMeshPrimitive) {
      if (nodeDrawLimit != 0) {
        nodeDrawLimit--;
        RenderMeshPrimitive nodeObj = (RenderMeshPrimitive) node;
        if (nodeObj.getMaterial().getAlphaMode() == GLTFAlphaMode.BLEND) {
          transparentNodes.add(nodeObj);
        } else {
          drawRenderObject((RenderMeshPrimitive) node);
        }
      }
    } else if (drawInvisibleNodes) {
      if (nodeDrawLimit != 0) {
        nodeDrawLimit--;
        drawInvisibleNode(node);
      }
    }
    for (RenderNode child : node.getChildren()) {
      draw(child, transparentNodes);
    }
  }

  private void drawInvisibleNode(RenderNode node) {
    ShaderProgram shader = ShaderCache.getDebugShaderProgram();
    glUseProgram(shader.getProgramId());

    this.projMatrix = camera.getProjectionMatrix();
    this.viewMatrix = camera.getViewMatrix();
    projMatrix.mul(viewMatrix, viewProjectionMatrix);

    shader.setUniform("u_ViewProjectionMatrix", viewProjectionMatrix);
    shader.setUniform("u_ModelMatrix", node.getWorldTransform());
    shader.setUniform("u_NormalMatrix", node.getNormalMatrix());
    shader.setUniform("u_Exposure", 0.1f);
    shader.setUniform("u_Camera", camera.getPosition());

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, debugEle);
    glBindBuffer(GL_ARRAY_BUFFER, debugBuf);

    int positionAttribute = shader.getAttributeLocation("a_Position");

    glVertexAttribPointer(positionAttribute, 4, GL_DOUBLE, false, 0, 0);
    glEnableVertexAttribArray(positionAttribute);

    glDrawElements(GL_TRIANGLES, 32, GL_UNSIGNED_SHORT, 0);

    glDisableVertexAttribArray(positionAttribute);
  }

  private void drawRenderObject(RenderMeshPrimitive rmp) {
    if (rmp.isSkip()) {
      return;
    }
    //select shader permutation, compile and link program.
    List<String> vertDefines = new ArrayList<>();
    pushVertParameterDefines(vertDefines, rmp);
    vertDefines.addAll(rmp.getDefines());

    RenderMaterial material = rmp.getMaterial();

    List<String> fragDefines = new ArrayList<>();
    fragDefines.addAll(vertDefines);//Add all the vert defines, some are needed
    fragDefines.addAll(material.getDefines());
    if (usePunctualLighting) {
      fragDefines.add("USE_PUNCTUAL 1");
      fragDefines.add("LIGHT_COUNT " + visibleLights.size());
    }
    if (useIBL) {
      fragDefines.add("USE_IBL 1");
      fragDefines.add("USE_TEX_LOD 1");
      fragDefines.add("USE_HDR 1");
    }

    //DEBUG
    if (debugType != ShaderDebugType.NONE) {
      fragDefines.add("DEBUG_OUTPUT 1");
      fragDefines.add(debugType.getDefine());
    }

    int vertexHash = ShaderCache.selectShader(rmp.getShaderIdentifier(), vertDefines);
    int fragmentHash = ShaderCache.selectShader(material.getShaderIdentifier(), fragDefines);

    ShaderProgram shader = ShaderCache.getShaderProgram(vertexHash, fragmentHash);

    glUseProgram(shader.getProgramId());

    if (usePunctualLighting) {
      //applyLights
      List<UniformLight> uniformLights = new ArrayList<>();
      for (RenderLight light : visibleLights) {
        uniformLights.add(light.getUniformLight());
      }
      shader.setUniform("u_Lights", uniformLights);
    }

    camera.updatePosition();

    this.projMatrix = camera.getProjectionMatrix();
    this.viewMatrix = camera.getViewMatrix();
    projMatrix.mul(viewMatrix, viewProjectionMatrix);

    //Assert viewProjectionMatrix is filled out
    assert (!viewProjectionMatrix.toString().contains("nan"));

    shader.setUniform("u_ViewProjectionMatrix", viewProjectionMatrix);
    shader.setUniform("u_ModelMatrix", rmp.getWorldTransform());
    shader.setUniform("u_NormalMatrix", rmp.getNormalMatrix());
    shader.setUniform("u_Exposure", 1.0f);
    shader.setUniform("u_Camera", camera.getPosition());

    boolean drawIndexed = rmp.getPrimitive().getIndicesAccessor().isPresent();

    if (drawIndexed) {
      GlUtil.setIndices(rmp.getPrimitive().getIndicesAccessor().get());
    }

    updateAnimationUniforms(shader, rmp.getMesh(), rmp);

    if (material.getGLTFMaterial().isDoubleSided()) {
      glDisable(GL_CULL_FACE);
    } else {
      glEnable(GL_CULL_FACE);
    }

    if (material.getGLTFMaterial().getAlphaMode() == GLTFAlphaMode.BLEND) {
      glEnable(GL_BLEND);
      glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
      glBlendEquation(GL_FUNC_ADD);
    } else {
      glDisable(GL_BLEND);
    }

    int vertexCount = 0;
    for (Entry<String, GLTFAccessor> entry : rmp.getGlAttributes().entrySet()) {
      String attributeName = entry.getKey();
      GLTFAccessor accessor = entry.getValue();

      vertexCount = accessor.getElementCount();

      int location = shader.getAttributeLocation(attributeName);

      if (location < 0) {
        continue;
      }
      GlUtil.enableAttribute(location, accessor);
    }

    for (Entry<String, Object> entry : material.getProperties().entrySet()) {
      shader.setUniform(entry.getKey(), entry.getValue());
    }

    int texSlot = 1;
    for (Entry<String, RenderTexture> entry : material.getTexturesMap()
        .entrySet()) {
      RenderTexture info = entry.getValue();
      int location = shader.getUniformLocation(entry.getKey());

      if (location < 0) {
        continue;
      }
      GlUtil.setTexture(location, info, texSlot++);
    }

    if (useIBL) {
      applyEnvironmentMap(shader, this.envData, texSlot);
    }

    if (drawIndexed) {
      GLTFAccessor indexAccessor = rmp.getPrimitive().getIndicesAccessor().get();
      glDrawElements(rmp.getPrimitive().getMode(), indexAccessor.getElementCount(),
          indexAccessor.getGLType(), 0);
    } else {
      glDrawArrays(rmp.getPrimitive().getMode(), 0, vertexCount);
    }

    for (String attribute : rmp.getGlAttributes().keySet()) {
      int location = shader.getAttributeLocation(attribute);
      if (location < 0) {
        continue;
      }
      glDisableVertexAttribArray(location);
    }
  }

  private void updateAnimationUniforms(ShaderProgram program, RenderMesh mesh,
      RenderMeshPrimitive renderMeshPrimitive) {

    // Skinning
    if (mesh.getSkin().isPresent()) {
      RenderSkin skin = mesh.getSkin().get();

      program.setUniform("u_jointMatrix", skin.getJointMatrices());
      program.setUniform("u_jointNormalMatrix", skin.getJointNormalMatrices());
    }

    if (renderMeshPrimitive.getPrimitive().getMorphTargets() != null
        && renderMeshPrimitive.getPrimitive().getMorphTargets().size() > 0) {
      if (mesh.getWeights() != null && mesh.getWeights().length > 0) {
        program.setUniform("u_morphWeights", mesh.getWeights());
      }
    }
  }

  private void pushVertParameterDefines(List<String> vertDefines,
      RenderMeshPrimitive renderMeshPrimitive) {
    //Skinning
    if (renderMeshPrimitive.getMesh().getSkin().isPresent()) {
      RenderSkin skin = renderMeshPrimitive.getMesh().getSkin().get();
      vertDefines.add("USE_SKINNING 1");
      vertDefines.add("JOINT_COUNT " + skin.getJointCount());
    }

    //Morphing
    if (renderMeshPrimitive.getPrimitive().getMorphTargets() != null
        && renderMeshPrimitive.getPrimitive().getMorphTargets().size() > 0) {
      RenderMesh mesh = renderMeshPrimitive.getMesh();
      if (mesh.getWeights() != null && mesh.getWeights().length > 0) {
        vertDefines.add("USE_MORPHING 1");
        vertDefines.add("WEIGHT_COUNT " + Math.min(mesh.getWeights().length, 8));
      }
    }
  }

  private void applyEnvironmentMap(ShaderProgram shader, RenderEnvironmentMap envData,
      int texSlotOffset) {
    GlUtil.setCubeMap(shader, envData, texSlotOffset);
    if (generateMipmaps) {
      shader.setUniform("u_MipCount", 10); //TODO global setting for mip count
    }
  }

  public ShaderDebugType getDebugType() {
    return debugType;
  }

  public void setDebugType(ShaderDebugType debugType) {
    this.debugType = debugType;
  }
}
