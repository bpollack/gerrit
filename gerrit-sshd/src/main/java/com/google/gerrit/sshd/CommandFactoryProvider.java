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

package com.google.gerrit.sshd;

import com.google.gerrit.sshd.SshScopes.Context;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.SessionAware;
import org.apache.sshd.server.session.ServerSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Creates a CommandFactory using commands registered by {@link CommandModule}.
 */
class CommandFactoryProvider implements Provider<CommandFactory> {
  private final DispatchCommandProvider dispatcher;
  private final SshLog log;

  @Inject
  CommandFactoryProvider(
      @CommandName(Commands.ROOT) final DispatchCommandProvider d,
      final SshLog l) {
    dispatcher = d;
    log = l;
  }

  @Override
  public CommandFactory get() {
    return new CommandFactory() {
      public Command createCommand(final String requestCommand) {
        return new Trampoline(requestCommand);
      }
    };
  }

  private class Trampoline implements Command, SessionAware {
    private final String commandLine;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback exit;
    private ServerSession session;
    private Context ctx;
    private DispatchCommand cmd;
    private boolean logged;

    Trampoline(final String cmdLine) {
      commandLine = cmdLine;
    }

    public void setInputStream(final InputStream in) {
      this.in = in;
    }

    public void setOutputStream(final OutputStream out) {
      this.out = out;
    }

    public void setErrorStream(final OutputStream err) {
      this.err = err;
    }

    public void setExitCallback(final ExitCallback callback) {
      this.exit = callback;
    }

    public void setSession(final ServerSession session) {
      this.session = session;
    }

    public void start(final Environment env) throws IOException {
      synchronized (this) {
        final Context old = SshScopes.current.get();
        try {
          ctx = new Context(session);
          SshScopes.current.set(ctx);

          cmd = dispatcher.get();
          cmd.setCommandLine(commandLine);
          cmd.setInputStream(in);
          cmd.setOutputStream(out);
          cmd.setErrorStream(err);
          cmd.setExitCallback(new ExitCallback() {
            @Override
            public void onExit(int rc, String exitMessage) {
              exit.onExit(translateExit(rc), exitMessage);
              log(rc);
            }

            @Override
            public void onExit(int rc) {
              exit.onExit(translateExit(rc));
              log(rc);
            }
          });
          cmd.start(env);
        } finally {
          SshScopes.current.set(old);
        }
      }
    }

    private int translateExit(final int rc) {
      switch (rc) {
        case BaseCommand.STATUS_NOT_ADMIN:
          return 1;

        case BaseCommand.STATUS_CANCEL:
          return 15 /* SIGKILL */;

        case BaseCommand.STATUS_NOT_FOUND:
          return 127 /* POSIX not found */;

        default:
          return rc;
      }
    }

    private void log(final int rc) {
      synchronized (this) {
        if (!logged) {
          ctx.finished = System.currentTimeMillis();
          log.onExecute(ctx, commandLine, rc);
          logged = true;
        }
      }
    }

    @Override
    public void destroy() {
      synchronized (this) {
        if (cmd != null) {
          final Context old = SshScopes.current.get();
          try {
            SshScopes.current.set(ctx);
            cmd.destroy();
            log(BaseCommand.STATUS_CANCEL);
          } finally {
            ctx = null;
            cmd = null;
            SshScopes.current.set(old);
          }
        }
      }
    }
  }
}
