/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.helium;

import static org.apache.zeppelin.helium.HeliumPackage.newHeliumPackage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.AbstractInterpreterTest;
import org.apache.zeppelin.interpreter.InterpreterException;
import org.apache.zeppelin.interpreter.InterpreterNotFoundException;
import org.apache.zeppelin.interpreter.InterpreterSetting;
import org.apache.zeppelin.notebook.ApplicationState;
import org.apache.zeppelin.notebook.AuthorizationService;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteManager;
import org.apache.zeppelin.notebook.Notebook;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.apache.zeppelin.user.Credentials;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class HeliumApplicationFactoryTest extends AbstractInterpreterTest {

  private NotebookRepo notebookRepo;
  private Notebook notebook;
  private HeliumApplicationFactory heliumAppFactory;
  private AuthenticationInfo anonymous;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();

    // set AppEventListener properly
    for (InterpreterSetting interpreterSetting : interpreterSettingManager.get()) {
      interpreterSetting.setAppEventListener(heliumAppFactory);
    }

    AuthorizationService authorizationService = mock(AuthorizationService.class);
    notebookRepo = mock(NotebookRepo.class);
    notebook =
        new Notebook(
            conf,
            authorizationService,
            notebookRepo,
            new NoteManager(notebookRepo, ZeppelinConfiguration.create()),
            interpreterFactory,
            interpreterSettingManager,
            new Credentials());

    heliumAppFactory = new HeliumApplicationFactory(notebook, null);

    notebook.addNotebookEventListener(heliumAppFactory);

    anonymous = new AuthenticationInfo("anonymous");
  }

  @Override
  @AfterEach
  public void tearDown() throws Exception {
    super.tearDown();
  }


  @Test
  @Disabled
  public void testLoadRunUnloadApplication()
      throws IOException, ApplicationException, InterruptedException {
    // given
    HeliumPackage pkg1 = newHeliumPackage(HeliumType.APPLICATION,
        "name1",
        "desc1",
        "",
        HeliumTestApplication.class.getName(),
        new String[][]{},
        "", "");

    String note1Id = notebook.createNote("note1", anonymous);
    Note note1 = notebook.processNote(note1Id,
      note1Tmp -> {
        return note1Tmp;
      });
    Paragraph p1 = note1.addNewParagraph(AuthenticationInfo.ANONYMOUS);
    // make sure interpreter process running
    p1.setText("%mock1 job");
    p1.setAuthenticationInfo(anonymous);
    note1.run(p1.getId());

    while(p1.isTerminated()==false || p1.getReturn()==null) Thread.yield();

    assertEquals("repl1: job", p1.getReturn().message().get(0).getData());

    // when
    assertEquals(0, p1.getAllApplicationStates().size());
    String appId = heliumAppFactory.loadAndRun(pkg1, p1);
    assertEquals(1, p1.getAllApplicationStates().size());
    ApplicationState app = p1.getApplicationState(appId);
    Thread.sleep(500); // wait for enough time

    // then
    assertEquals("Hello world 1", app.getOutput());

    // when
    heliumAppFactory.run(p1, appId);
    Thread.sleep(500); // wait for enough time

    // then
    assertEquals("Hello world 2", app.getOutput());

    // clean
    heliumAppFactory.unload(p1, appId);
    notebook.removeNote(note1.getId(), anonymous);
  }

  @Test
  @Disabled
  public void testUnloadOnParagraphRemove() throws IOException {
    // given
    HeliumPackage pkg1 = newHeliumPackage(HeliumType.APPLICATION,
        "name1",
        "desc1",
        "",
        HeliumTestApplication.class.getName(),
        new String[][]{},
        "", "");

    String note1Id = notebook.createNote("note1", anonymous);
    Note note1 = notebook.processNote(note1Id,
      note1Tmp -> {
        return note1Tmp;
      });
    Paragraph p1 = note1.addNewParagraph(AuthenticationInfo.ANONYMOUS);

    // make sure interpreter process running
    p1.setText("%mock1 job");
    p1.setAuthenticationInfo(anonymous);
    note1.run(p1.getId());
    while(p1.isTerminated()==false || p1.getReturn()==null) Thread.yield();

    assertEquals(0, p1.getAllApplicationStates().size());
    String appId = heliumAppFactory.loadAndRun(pkg1, p1);
    ApplicationState app = p1.getApplicationState(appId);
    while (app.getStatus() != ApplicationState.Status.LOADED) {
      Thread.yield();
    }

    // when remove paragraph
    note1.removeParagraph("user", p1.getId());

    // then
    assertEquals(ApplicationState.Status.UNLOADED, app.getStatus());

    // clean
    notebook.removeNote(note1.getId(), anonymous);
  }


  @Test
  @Disabled
  public void testUnloadOnInterpreterUnbind() throws IOException {
    // given
    HeliumPackage pkg1 = newHeliumPackage(HeliumType.APPLICATION,
        "name1",
        "desc1",
        "",
        HeliumTestApplication.class.getName(),
        new String[][]{},
        "", "");

    String note1Id = notebook.createNote("note1", anonymous);
    Note note1 = notebook.processNote(note1Id,
      note1Tmp -> {
        return note1Tmp;
      });

    Paragraph p1 = note1.addNewParagraph(AuthenticationInfo.ANONYMOUS);

    // make sure interpreter process running
    p1.setText("%mock1 job");
    p1.setAuthenticationInfo(anonymous);
    note1.run(p1.getId());
    while(p1.isTerminated()==false || p1.getReturn()==null) Thread.yield();

    assertEquals(0, p1.getAllApplicationStates().size());
    String appId = heliumAppFactory.loadAndRun(pkg1, p1);
    ApplicationState app = p1.getApplicationState(appId);
    while (app.getStatus() != ApplicationState.Status.LOADED) {
      Thread.yield();
    }

    // then
    assertEquals(ApplicationState.Status.UNLOADED, app.getStatus());

    // clean
    notebook.removeNote(note1.getId(), anonymous);
  }

  @Test
  @Disabled
  public void testInterpreterUnbindOfNullReplParagraph() throws IOException {
    // create note
    String note1Id = notebook.createNote("note1", anonymous);
    Note note1 = notebook.processNote(note1Id,
      note1Tmp -> {
        return note1Tmp;
      });

    // add paragraph with invalid magic
    Paragraph p1 = note1.addNewParagraph(AuthenticationInfo.ANONYMOUS);
    p1.setText("%fake ");

    // make sure that p1's repl is null
    try {
      p1.getBindedInterpreter();
      fail("Should throw InterpreterNotFoundException");
    } catch (InterpreterNotFoundException e) {

    }

    // remove note
    notebook.removeNote(note1.getId(), anonymous);
  }


  @Test
  @Disabled
  public void testUnloadOnInterpreterRestart() throws IOException, InterpreterException {
    // given
    HeliumPackage pkg1 = newHeliumPackage(HeliumType.APPLICATION,
        "name1",
        "desc1",
        "",
        HeliumTestApplication.class.getName(),
        new String[][]{},
        "", "");

    String note1Id = notebook.createNote("note1", anonymous);
    Note note1 = notebook.processNote(note1Id,
      note1Tmp -> {
        return note1Tmp;
      });
    String mock1IntpSettingId = null;
    for (InterpreterSetting setting : note1.getBindedInterpreterSettings(new ArrayList<>())) {
      if (setting.getName().equals("mock1")) {
        mock1IntpSettingId = setting.getId();
        break;
      }
    }

    Paragraph p1 = note1.addNewParagraph(AuthenticationInfo.ANONYMOUS);

    // make sure interpreter process running
    p1.setText("%mock1 job");
    p1.setAuthenticationInfo(anonymous);
    note1.run(p1.getId());
    while(p1.isTerminated()==false || p1.getReturn()==null) Thread.yield();
    assertEquals(0, p1.getAllApplicationStates().size());
    String appId = heliumAppFactory.loadAndRun(pkg1, p1);
    ApplicationState app = p1.getApplicationState(appId);
    while (app.getStatus() != ApplicationState.Status.LOADED) {
      Thread.yield();
    }
    // wait until application is executed
    while (!"Hello world 1".equals(app.getOutput())) {
      Thread.yield();
    }
    // when restart interpreter
    interpreterSettingManager.restart(mock1IntpSettingId);
    while (app.getStatus() == ApplicationState.Status.LOADED) {
      Thread.yield();
    }
    // then
    assertEquals(ApplicationState.Status.UNLOADED, app.getStatus());

    // clean
    notebook.removeNote(note1.getId(), anonymous);
  }
}
