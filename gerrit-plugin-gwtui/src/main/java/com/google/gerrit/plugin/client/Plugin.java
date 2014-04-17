// Copyright (C) 2013 The Android Open Source Project
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

package com.google.gerrit.plugin.client;

import com.google.gerrit.plugin.client.screen.Screen;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * Wrapper around the plugin instance exposed by Gerrit.
 *
 * Listeners for events generated by the main UI must be registered
 * through this instance.
 */
public final class Plugin extends JavaScriptObject {
  private static final Plugin self = install(
      GWT.getModuleBaseURL() + GWT.getModuleName() + ".nocache.js");

  /** Obtain the plugin instance wrapper. */
  public static Plugin get() {
    return self;
  }

  /** Installed name of the plugin. */
  public final String getName() {
    return getPluginName();
  }

  /** Installed name of the plugin. */
  public final native String getPluginName()
  /*-{ return this.getPluginName() }-*/;

  /** Navigate the UI to the screen identified by the token. */
  public final native void go(String token)
  /*-{ return this.go(token) }-*/;

  /** Refresh the current UI. */
  public final native void refresh()
  /*-{ return this.refresh() }-*/;

  /** Refresh Gerrit's menu bar. */
  public final native void refreshMenuBar()
  /*-{ return this.refreshMenuBar() }-*/;

  /** Show message in Gerrit's ErrorDialog. */
  public final native void showError(String message)
  /*-{ return this.showError(message) }-*/;

  /**
   * Register a screen displayed at {@code /#/x/plugin/token}.
   *
   * @param token literal anchor token appearing after the plugin name. For
   *        regular expression matching use {@code screenRegex()} .
   * @param entry callback function invoked to create the screen widgets.
   */
  public final void screen(String token, Screen.EntryPoint entry) {
    screen(token, wrap(entry));
  }

  private final native void screen(String t, JavaScriptObject e)
  /*-{ this.screen(t, e) }-*/;

  /**
   * Register a screen displayed at {@code /#/x/plugin/regex}.
   *
   * @param regex JavaScript {@code RegExp} expression to match the anchor token
   *        after the plugin name. Matching groups are exposed through the
   *        {@code Screen} object passed into the {@code Screen.EntryPoint}.
   * @param entry callback function invoked to create the screen widgets.
   */
  public final void screenRegex(String regex, Screen.EntryPoint entry) {
    screenRegex(regex, wrap(entry));
  }

  private final native void screenRegex(String p, JavaScriptObject e)
  /*-{ this.screen(new $wnd.RegExp(p), e) }-*/;

  protected Plugin() {
  }

  native void _initialized() /*-{ this._success = true }-*/;
  native void _loaded() /*-{ this._loadedGwt() }-*/;
  private static native final Plugin install(String u)
  /*-{ return $wnd.Gerrit.installGwt(u) }-*/;

  private static final native JavaScriptObject wrap(Screen.EntryPoint b) /*-{
    return $entry(function(c){
      b.@com.google.gerrit.plugin.client.screen.Screen.EntryPoint::onLoad(Lcom/google/gerrit/plugin/client/screen/Screen;)(
        @com.google.gerrit.plugin.client.screen.Screen::new(Lcom/google/gerrit/plugin/client/screen/Screen$Context;)(c));
    });
  }-*/;
}
