/**
 * The MIT License
 *
 * Copyright (C) 2015 Asterios Raptis
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.astrapi69.mystic.crypt.panel.dbtree;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.kquiet.browser.ActionComposer;
import org.kquiet.browser.ActionRunner;
import org.kquiet.browser.BrowserType;
import org.kquiet.concurrent.PausablePriorityThreadPoolExecutor;
import org.kquiet.concurrent.PriorityRunnable;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.remote.CapabilityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MyBasicActionRunner} maintains two prioritized thread pool internally to execute
 * {@link ActionComposer} and browser actions separatedly. The thread pool for
 * {@link ActionComposer} allows multiple ones to be run concurrently(depends on parameter values in
 * constructors). The thread pool for browser actions is single-threaded, so only one browser action
 * is executed at a time(due to <a href=
 * "https://github.com/SeleniumHQ/selenium/wiki/Frequently-Asked-Questions#q-is-webdriver-thread-safe"
 * target="_blank"> the constraint of WebDriver</a>).
 *
 * @author Kimberly
 */
public class MyBasicActionRunner implements ActionRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyBasicActionRunner.class);

  private String name = "";
  private final WebDriver brsDriver;
  private final String rootWindowIdentity;

  private volatile boolean isPaused = false;

  private final PausablePriorityThreadPoolExecutor browserActionExecutor;
  private final PausablePriorityThreadPoolExecutor composerExecutor;

  /**
   * Create an {@link MyBasicActionRunner} with {@link PageLoadStrategy#NONE} as page load strategy,
   * {@link BrowserType#CHROME} as browser type, and at most one {@link ActionComposer} is allowed
   * to be executed at a time.
   */
  public MyBasicActionRunner() {
    this(1);
  }

  /**
   * Create an {@link MyBasicActionRunner} with {@link PageLoadStrategy#NONE} as page load strategy
   * and {@link BrowserType#CHROME} as browser type.
   *
   * @param maxConcurrentComposer the max number of {@link ActionComposer} that could be executed by
   *        this {@link MyBasicActionRunner} concurrently.
   */
  public MyBasicActionRunner(int maxConcurrentComposer) {
    this(PageLoadStrategy.NONE, BrowserType.CHROME, maxConcurrentComposer);
  }

  /**
   * Create an {@link MyBasicActionRunner} with specified page load strategy, browser type, name, and
   * at most one {@link ActionComposer} is allowed to be executed at a time.
   *
   * @param pageLoadStrategy page load strategy
   * @param browserType the type of browser
   * @see <a href=
   *      "https://github.com/SeleniumHQ/selenium/blob/master/java/client/src/org/openqa/selenium/PageLoadStrategy.java"
   *      target="_blank"> possible page load strategies </a> and
   *      <a href="https://w3c.github.io/webdriver/#dfn-table-of-page-load-strategies" target=
   *      "_blank">corresponding document readiness</a>
   */
  public MyBasicActionRunner(PageLoadStrategy pageLoadStrategy, BrowserType browserType) {
    this(pageLoadStrategy, browserType, 1);
  }

  /**
   * Create an {@link MyBasicActionRunner} with specified page load strategy, browser type, name, and
   * max number of concurrent composer.
   *
   * @param pageLoadStrategy page load strategy
   * @param browserType the type of browser
   * @param maxConcurrentComposer max number of {@link ActionComposer} that could be executed
   *        concurrently
   * @see <a href=
   *      "https://github.com/SeleniumHQ/selenium/blob/master/java/client/src/org/openqa/selenium/PageLoadStrategy.java"
   *      target="_blank"> possible page load strategies </a> and
   *      <a href="https://w3c.github.io/webdriver/#dfn-table-of-page-load-strategies" target=
   *      "_blank">corresponding document readiness</a>
   */
  public MyBasicActionRunner(PageLoadStrategy pageLoadStrategy, BrowserType browserType,
                             int maxConcurrentComposer) {
    this.browserActionExecutor =
        new PausablePriorityThreadPoolExecutor("BrowserActionExecutorPool", 1, 1);
    this.composerExecutor = new PausablePriorityThreadPoolExecutor("ActionComposerExecutorPool",
        maxConcurrentComposer, maxConcurrentComposer);

    this.brsDriver = createBrowserDriver(browserType, pageLoadStrategy);

    this.brsDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(1));
    this.rootWindowIdentity = this.brsDriver.getWindowHandle();
  }

  private static WebDriver createBrowserDriver(BrowserType browserType,
      PageLoadStrategy pageLoadStrategy) {
    Capabilities extraCapabilities = new ImmutableCapabilities(CapabilityType.PAGE_LOAD_STRATEGY,
        pageLoadStrategy.toString().toLowerCase());
    switch (browserType) {
      case CHROME:
        ChromeOptions chromeOption = new ChromeOptions();
        chromeOption.setExperimentalOption("detach", Boolean.TRUE);
        List<String> arguments = List.of(
            Optional.ofNullable(System.getProperty("chrome_option_args")).orElse(",").split(","));
        if (arguments.size() > 0) {
          chromeOption.addArguments(arguments);
        }

        if ("yes".equalsIgnoreCase(System.getProperty("webdriver_headless"))) {
          chromeOption.addArguments("--headless=new");
          LOGGER.info("headless chrome used");
        }
        if ("yes".equalsIgnoreCase(System.getProperty("webdriver_use_default_user_data_dir"))) {
          String defaultUserDataDir = getDefaultUserDataDir(browserType);
          if (defaultUserDataDir != null) {
            chromeOption.addArguments("user-data-dir=" + defaultUserDataDir);
            LOGGER.info("default user data dir used:{}", defaultUserDataDir);
          }
        }
        return new ChromeDriver(chromeOption.merge(extraCapabilities));
      case FIREFOX:
      default:
        FirefoxOptions firefoxOption = new FirefoxOptions();
        if ("yes".equalsIgnoreCase(System.getProperty("webdriver_headless"))) {
          firefoxOption.addArguments("-headless");
          LOGGER.info("headless firefox used");
        }
        if ("yes".equalsIgnoreCase(System.getProperty("webdriver_use_default_user_data_dir"))) {
          String defaultProfileDir = getDefaultUserDataDir(browserType);
          if (defaultProfileDir != null) {
            firefoxOption.setProfile(new FirefoxProfile(new File(defaultProfileDir)));
            LOGGER.info("default user profile dir used:{}", defaultProfileDir);
          }
        }
        return new FirefoxDriver(firefoxOption.merge(extraCapabilities));
    }
  }

  private static String getDefaultUserDataDir(BrowserType browserType) {
    boolean isWindows = Optional.ofNullable(System.getProperty("os.name")).orElse("")
        .toLowerCase(Locale.ENGLISH).startsWith("windows");
    String path;
    switch (browserType) {
      case CHROME:
        if (isWindows) {
          path = System.getenv("LOCALAPPDATA") + "\\Google\\Chrome\\User Data";
          if (!new File(path).isDirectory()) {
            path = System.getenv("LOCALAPPDATA") + "\\Chromium\\User Data";
          }
        } else {
          path = "~/.config/google-chrome";
          if (!new File(path).isDirectory()) {
            path = "~/.config/chromium";
          }
        }
        if (!new File(path).isDirectory()) {
          path = null;
        }
        break;
      case FIREFOX:
        if (isWindows) {
          path = System.getenv("APPDATA") + "\\Mozilla\\Firefox\\Profiles";
        } else {
          path = "~/.mozilla/firefox";
        }
        File pathFile = new File(path);
        if (!pathFile.isDirectory()) {
          path = null;
        } else {
          String[] dirs = pathFile.list((current, name) -> new File(current, name).isDirectory()
              && name.toLowerCase().endsWith(".default"));
          if (dirs != null && dirs.length > 0) {
            path = new File(pathFile, dirs[0]).getAbsolutePath();
          } else {
            path = null;
          }
        }
        break;
      default:
        throw new UnsupportedOperationException(
            "Unsupported browser type:" + browserType.toString());
    }
    return path;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public MyBasicActionRunner setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String getRootWindowIdentity() {
    return rootWindowIdentity;
  }

  @Override
  public CompletableFuture<Void> executeAction(Runnable browserAction, int priority) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    PriorityRunnable runnable = new PriorityRunnable(() -> {
      try {
        browserAction.run();
        future.complete(null);
      } catch (Exception ex) {
        future.completeExceptionally(ex);
      }
    }, priority);
    browserActionExecutor.submit(runnable);
    return future;
  }

  @Override
  public CompletableFuture<Void> executeComposer(ActionComposer actionComposer) {
    actionComposer.setActionRunner(this);
    CompletableFuture<Void> future = new CompletableFuture<>();
    PriorityRunnable runnable = new PriorityRunnable(() -> {
      try {
        actionComposer.run();
        future.complete(null);
      } catch (Exception ex) {
        future.completeExceptionally(ex);
      }
    }, actionComposer.getPriority());
    composerExecutor.submit(runnable);
    return future;
  }

  @Override
  public WebDriver getWebDriver() {
    return brsDriver;
  }

  /**
   * This method checks the existence of root window and use the result as the result of browser's
   * aliveness. When a negative result is acquired, users can choose to {@link #close() close}
   * {@link ActionRunner} and create a new one.
   *
   * @return {@code true} if the root window exists; {@code false} otherwise
   */
  @Override
  public boolean isBrowserAlive() {
    if (brsDriver == null) {
      return false;
    }

    try {
      Set<String> windowSet = brsDriver.getWindowHandles();
      return (windowSet != null && !windowSet.isEmpty()
          && windowSet.contains(getRootWindowIdentity()));
    } catch (Exception ex) {
      LOGGER.warn("[{}] check browser alive error!", name, ex);
      return false;
    }
  }

  /**
   * Stop executing queued {@link ActionComposer} or browser action, but the running ones will keep
   * running.
   */
  @Override
  public void pause() {
    if (isPaused) {
      return;
    }

    isPaused = true;
    browserActionExecutor.pause();
    composerExecutor.pause();
  }

  /** Resume to executing queued {@link ActionComposer} or browser action (if any). */
  @Override
  public void resume() {
    if (!isPaused) {
      return;
    }

    isPaused = false;
    browserActionExecutor.resume();
    composerExecutor.resume();
  }

  @Override
  public boolean isPaused() {
    return isPaused;
  }

  @Override
  public void close() {
    // prevents to close the browser!!
  }
}
