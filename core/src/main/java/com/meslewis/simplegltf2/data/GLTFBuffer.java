/*
 * Copyright (c) 2020. Morgan Lewis <https://github.com/MESLewis>
 * This file is part of the SimpleGLTF2 project which is released under the MIT License.
 * See <https://github.com/MESLewis/SimpleGLTF2/blob/master/LICENSE> for more information
 */

package com.meslewis.simplegltf2.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.net.URI;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * A buffer points to binary geometry, animation, or skins. TODO some buffers can be copied directly
 * to GPU
 */
public class GLTFBuffer extends GLTFChildOfRootProperty {

  /**
   * The uri of the buffer. Relative paths are relative to the .gltf file. Instead of referencing an
   * external file, the uri can also be a data-uri. Will be null if referencing a glb buffer
   */
  @JsonProperty("uri")
  private URI uri;

  /**
   * The length of the buffer in bytes.
   */
  @JsonProperty("byteLength")
  @NotNull
  @Min(1)
  private int byteLength = -1;
  /**
   * Java nio Buffer holding data
   */
  private ByteBuffer buffer;

  public URI getUri() {
    return uri;
  }

  public int getByteLength() {
    return byteLength;
  }

  /**
   * @return the String for this buffer's URI
   */
  public String getScheme() {
    return this.uri.getScheme();
  }

  /**
   * Load the data referenced by this Buffer into a java.nio.Buffer
   *
   * @return java.nio.Buffer with relevant data
   */
  public ByteBuffer getData(int start, int length) {
    if (start + length > this.byteLength) {
      throw new BufferUnderflowException();
    }
    if (buffer == null) {
      resolveBufferData();
    }
    return buffer.slice(start, length).order(ByteOrder.LITTLE_ENDIAN);
  }

  /**
   * Loads the buffer according to uri
   * <p>
   * if URI is underfined it must be referencing the bin chunk of this glb
   *
   * @throws IOException
   */
  private void resolveBufferData() {
    this.buffer = URIUtil.getDirectBufferFromGeneralURI(gltf, uri);
    //All glTF buffers are little endian
    assert (this.buffer.order() == ByteOrder.LITTLE_ENDIAN);
  }
}
