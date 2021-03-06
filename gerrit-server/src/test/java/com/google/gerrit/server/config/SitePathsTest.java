// Copyright (C) 2009 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.gerrit.server.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.gerrit.server.util.HostPlatform;

import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class SitePathsTest {
  @Test
  public void testCreate_NotExisting() throws IOException {
    final File root = random();
    final SitePaths site = new SitePaths(root);
    assertTrue(site.isNew);
    assertEquals(root, site.site_path);
    assertEquals(new File(root, "etc"), site.etc_dir);
  }

  @Test
  public void testCreate_Empty() throws IOException {
    final File root = random();
    try {
      assertTrue(root.mkdir());

      final SitePaths site = new SitePaths(root);
      assertTrue(site.isNew);
      assertEquals(root, site.site_path);
    } finally {
      root.delete();
    }
  }

  @Test
  public void testCreate_NonEmpty() throws IOException {
    final File root = random();
    final File txt = new File(root, "test.txt");
    try {
      assertTrue(root.mkdir());
      assertTrue(txt.createNewFile());

      final SitePaths site = new SitePaths(root);
      assertFalse(site.isNew);
      assertEquals(root, site.site_path);
    } finally {
      txt.delete();
      root.delete();
    }
  }

  @Test
  public void testCreate_NotDirectory() throws IOException {
    final File root = random();
    try {
      assertTrue(root.createNewFile());
      try {
        new SitePaths(root);
        fail("Did not throw exception");
      } catch (FileNotFoundException e) {
        assertEquals("Not a directory: " + root.getPath(), e.getMessage());
      }
    } finally {
      root.delete();
    }
  }

  @Test
  public void testResolve() throws IOException {
    final File root = random();
    final SitePaths site = new SitePaths(root);

    assertNull(site.resolve(null));
    assertNull(site.resolve(""));

    assertNotNull(site.resolve("a"));
    assertEquals(new File(root, "a").getCanonicalFile(), site.resolve("a"));

    final String pfx = HostPlatform.isWin32() ? "C:/" : "/";
    assertNotNull(site.resolve(pfx + "a"));
    assertEquals(new File(pfx + "a").getCanonicalFile(), site.resolve(pfx + "a"));
  }

  private static File random() throws IOException {
    File tmp = File.createTempFile("gerrit_test_", "_site");
    if (!tmp.delete()) {
      throw new IOException("Cannot create " + tmp.getPath());
    }
    return tmp;
  }
}
