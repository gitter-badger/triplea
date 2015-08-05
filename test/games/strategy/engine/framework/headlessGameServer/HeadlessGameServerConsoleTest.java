package games.strategy.engine.framework.headlessGameServer;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;


import java.io.InputStream;
import java.io.PrintStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HeadlessGameServerConsoleTest {

  private HeadlessGameServerConsole testObj;

  @Mock
  private HeadlessGameServer mockHeadlessGameServer;

  @Mock
  private InputStream mockInputStream;

  @Mock
  private PrintStream mockPrintStream;

  @Before
  public void setUp() {
    testObj = new HeadlessGameServerConsole(mockHeadlessGameServer,
        mockInputStream, mockPrintStream);
  }

  @Test
  public void test() {
    final String testString = "testString";
    testObj.println(testString);
    verify( mockPrintStream, times(1)).println(testString);
  }


}
