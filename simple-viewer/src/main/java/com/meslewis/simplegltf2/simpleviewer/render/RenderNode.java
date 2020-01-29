/*
 * Copyright (c) 2020. Morgan Lewis <https://github.com/MESLewis>
 * This file is part of the SimpleGLTF2 project which is released under the MIT License.
 * See <https://github.com/MESLewis/SimpleGLTF2/blob/master/LICENSE> for more information
 */

package com.meslewis.simplegltf2.simpleviewer.render;

import com.meslewis.simplegltf2.data.GLTFNode;
import java.util.ArrayList;
import java.util.List;
import org.joml.AABBf;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class RenderNode {
  private GLTFNode gltfNode;
  private Vector3f scale = new Vector3f(1.0f, 1.0f, 1.0f);
  private Vector3f translation = new Vector3f();
  private Quaternionf rotation = new Quaternionf();
  private Matrix4f worldTransform = new Matrix4f();
  private Matrix4f inverseWorldTransform = new Matrix4f();
  private Matrix4f normalMatrix = new Matrix4f();
  private List<RenderNode> children = new ArrayList<>();
  private RenderNode parent;
  private boolean changed;

  private Matrix4f localTransform = null;
  protected AABBf boundingBox;

  public RenderNode(GLTFNode node, RenderNode parent) {
    this.gltfNode = node;
    this.parent = parent;

    if (node != null) {
      if (node.getMatrix() != null) {
        applyMatrix(node.getMatrix());
      } else {
        float[] scalef = node.getScale();
        scale = new Vector3f().set(scalef[0], scalef[1], scalef[2]);

        float[] rotf = node.getRotation();
        rotation = new Quaternionf().set(rotf[0], rotf[1], rotf[2], rotf[3]);

        float[] traf = node.getTranslation();
        translation = new Vector3f().set(traf[0], traf[1], traf[2]);
      }
    }

    //Register as child
    if (parent != null) {
      parent.addChild(this);
    }
  }

  void addChild(RenderNode child) {
    this.children.add(child);
  }

  public List<RenderNode> getChildren() {
    return this.children;
  }

  private void applyMatrix(float[] floatMatrix) {
    Matrix4f matrix = new Matrix4f().set(floatMatrix);
    localTransform = matrix;
    matrix.getScale(scale);
    matrix.getUnnormalizedRotation(rotation);
    matrix.getTranslation(translation);
    changed = true;
  }

  private Matrix4f getLocalTransform() {
    if (localTransform == null) {
      localTransform = new Matrix4f();
      changed = true;
    }
    if (changed) {
      localTransform.identity();
      localTransform.translationRotateScale(translation, rotation, scale);
      changed = false;
    }
    return this.localTransform;
  }

  public Matrix4f getWorldTransform() {
    assert (!changed);
    return this.worldTransform;
  }

  public void applyTransform(Matrix4f parentTransform) {
    Matrix4f localTransform = getLocalTransform();
    parentTransform.mul(localTransform, worldTransform);
    worldTransform.invert(inverseWorldTransform);
    inverseWorldTransform.transpose(normalMatrix);

    for (RenderNode child : children) {
      child.applyTransform(this.getWorldTransform());
    }
  }

  public GLTFNode getGltfNode() {
    return gltfNode;
  }

  public Matrix4f getNormalMatrix() {
    return normalMatrix;
  }

  /**
   * Get the axis aligned bounding box for this node
   *
   * @return
   */
  public AABBf getBoundingBox() {
    if (boundingBox == null) {
      boundingBox = new AABBf();
    }
    return boundingBox;
  }

  public Vector3f getTranslation() {
    this.changed = true;
    return this.translation;
  }

  public Vector3f getScale() {
    this.changed = true;
    return this.scale;
  }

  public Quaternionf getRotation() {
    this.changed = true;
    return this.rotation;
  }
}