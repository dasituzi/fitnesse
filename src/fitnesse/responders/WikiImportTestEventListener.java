// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse.responders;

import java.util.logging.Level;
import java.util.logging.Logger;

import fitnesse.responders.run.SuiteResponder;
import fitnesse.responders.run.TestEventListener;
import fitnesse.responders.run.TestResponder;
import fitnesse.wiki.PageData;
import fitnesse.wiki.WikiImportProperty;
import fitnesse.wiki.WikiPage;

public class WikiImportTestEventListener implements TestEventListener {
  private static final Logger LOG = Logger.getLogger(WikiImportTestEventListener.class.getName());

  public static void register() {
    TestResponder.registerListener(new WikiImportTestEventListener(new WikiImporterFactory()));
  }

  private WikiImporterFactory importerFactory;

  public WikiImportTestEventListener(WikiImporterFactory importerFactory) {
    this.importerFactory = importerFactory;
  }

  @Override
  public void notifyPreTest(TestResponder testResponder, PageData data) {
    TestEventProcessor eventProcessor;
    if (testResponder instanceof SuiteResponder)
      eventProcessor = new SuiteEventProcessor();
    else
      eventProcessor = new TestEventProcessor();

    eventProcessor.run(testResponder, data);
  }

  private class TestEventProcessor implements WikiImporterClient {
    private TestResponder testResponder;
    private boolean errorOccured;
    protected WikiImporter wikiImporter;
    protected WikiPage wikiPage;
    protected PageData data;
    protected WikiImportProperty importProperty;

    public void run(TestResponder testResponder, PageData data) {
      this.testResponder = testResponder;
      this.data = data;
      importProperty = WikiImportProperty.createFrom(data.getProperties());
      if (importProperty != null && importProperty.isAutoUpdate()) {
        announceImportAttempt(testResponder);
        doImport(testResponder, data);
        closeAnnouncement(testResponder);
      }
    }

    private void closeAnnouncement(TestResponder testResponder) {
      if (testResponder.getResponse().isHtmlFormat())
        testResponder.addToResponse("</span>");
    }

    private void announceImportAttempt(TestResponder testResponder) {
      if (testResponder.getResponse().isHtmlFormat()) {
        testResponder.addToResponse("<span class=\"meta\">Updating imported content...</span>");
        testResponder.addToResponse("<span class=\"meta\">");
      }
    }

    private void doImport(TestResponder testResponder, PageData data) {
      try {
        wikiImporter = importerFactory.newImporter(this);
        wikiImporter.parseUrl(importProperty.getSourceUrl());
        wikiPage = data.getWikiPage();

        doUpdating();

        if (!errorOccured)
          announceDone(testResponder);
      }
      catch (Exception e) {
        pageImportError(data.getWikiPage(), e);
      }
    }

    private void announceDone(TestResponder testResponder) {
      if (testResponder.getResponse().isHtmlFormat())
        testResponder.addToResponse("done");
    }

    protected void doUpdating() {
      updatePagePassedIn();
    }

    protected void updatePagePassedIn() {
      wikiImporter.importRemotePageContent(wikiPage);
      // FixMe: Ugle hack to ensure ChunkedResponder.data is updated as well as any child pages!
      data.setContent(wikiPage.getData().getContent());
    }

    @Override
    public void pageImported(WikiPage localPage) {
    }

    @Override
    public void pageImportError(WikiPage localPage, Exception e) {
      errorOccured = true;
      LOG.log(Level.WARNING, "Exception while importing \"local page\": " + localPage.getName(), e);
      testResponder.addToResponse(e.toString());
    }
  }

  private class SuiteEventProcessor extends TestEventProcessor {
    protected void doUpdating() {
      if (!importProperty.isRoot())
        updatePagePassedIn();
      wikiImporter.setAutoUpdateSetting(true);
      wikiImporter.importWiki(wikiPage);
    }
  }
}
