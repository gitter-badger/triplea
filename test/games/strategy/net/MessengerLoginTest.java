package games.strategy.net;

import games.strategy.engine.EngineVersion;
import games.strategy.engine.framework.startup.login.*;
import games.strategy.test.TestUtil;
import games.strategy.util.MD5Crypt;

import java.net.SocketAddress;
import java.util.*;

import junit.framework.TestCase;

public class MessengerLoginTest extends TestCase
{
    private int SERVER_PORT = -1;
    
    @Override
    public void setUp()
    {
        SERVER_PORT = TestUtil.getUniquePort();
    }
    
    
    
    public void testSimple() throws Exception
    {
        ILoginValidator validator = new ILoginValidator()
        {        
            @Override
			public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, String mac, SocketAddress remoteAddress)
            {
                return null;
            }
        
            @Override
			public Map<String,String> getChallengeProperties(String userName, SocketAddress remoteAddress)
            {
                return new HashMap<String,String>();
            }        
        };
        
        IConnectionLogin login = new IConnectionLogin()
        {
        
            @Override
			public void notifyFailedLogin(String message)
            {
                 fail();
            }
        
            @Override
			public Map<String, String> getProperties(Map<String,String> challengProperties)
            {
                return new HashMap<String,String>();
            }
        
        };
        
        
        ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
        try
        {
            server.setLoginValidator(validator);
            
            server.setAcceptNewConnections(true);
            
            String mac = MacFinder.GetHashedMacAddress();
            ClientMessenger client = new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
            
            client.shutDown();
        }
        finally
        {
            server.shutDown();            
        }

        
    }
    

    
    public void testRefused() throws Exception
    {
        ILoginValidator validator = new ILoginValidator()
        {        
            @Override
			public String verifyConnection(Map<String, String> propertiesSentToClient, Map<String, String> propertiesReadFromClient, String clientName, String mac, SocketAddress remoteAddress)
            {
                return "error";
            }
        
            @Override
			public Map<String,String> getChallengeProperties(String userName, SocketAddress remoteAddress)
            {
                return new HashMap<String,String>();
            }
        };
        
        IConnectionLogin login = new IConnectionLogin()
        {
        
            @Override
			public void notifyFailedLogin(String message)
            {
                 
            }
        
            @Override
			public Map<String, String> getProperties(Map<String,String> challengProperties)
            {
                return new HashMap<String,String>();
            }
        
        };
        
        
        ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
        try
        {
            server.setLoginValidator(validator);
            
            server.setAcceptNewConnections(true);
            
            try
            {
                String mac = MacFinder.GetHashedMacAddress();
                new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
                fail("we should not have logged in");
            }
            catch(CouldNotLogInException expected)
            {
                //we expect this exception
            }
            
            
        }
        finally
        {
            server.shutDown();            
        }

        
    }

    public void testGetMagic()
    {
        String salt = "falafel";
        String password = "king";
        String encrypted = MD5Crypt.crypt(password, salt, MD5Crypt.MAGIC);
        assertEquals(salt,MD5Crypt.getSalt(MD5Crypt.MAGIC, encrypted));
        
    }
    
    
    public void testPassword() throws Exception
    {
      
        ClientLoginValidator validator = new ClientLoginValidator();
        validator.setGamePassword("foo");
        
        IConnectionLogin login = new IConnectionLogin()
        {
        
            @Override
			public void notifyFailedLogin(String message)
            {
                 fail();
            }
        
            @Override
			public Map<String, String> getProperties(Map<String,String> challengProperties)
            {
                String salt = challengProperties.get(ClientLoginValidator.SALT_PROPERTY);
                
                HashMap<String,String> rVal = new HashMap<String,String>();
                rVal.put(ClientLogin.PASSWORD_PROPERTY, MD5Crypt.crypt("foo", salt));
                rVal.put(ClientLogin.ENGINE_VERSION_PROPERTY, EngineVersion.VERSION.toString());
                return rVal;
            }
        
        };
        
        
        ServerMessenger server = new ServerMessenger("test", SERVER_PORT);
        try
        {
            server.setLoginValidator(validator);
            
            server.setAcceptNewConnections(true);
            
            String mac = MacFinder.GetHashedMacAddress();
            ClientMessenger client = new ClientMessenger("localhost", SERVER_PORT, "fee", mac, new DefaultObjectStreamFactory(), login);
            
            client.shutDown();
        }
        finally
        {
            server.shutDown();            
        }

        
        
    }

}


