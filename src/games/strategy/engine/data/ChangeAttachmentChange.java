/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


package games.strategy.engine.data;

import games.strategy.util.PropertyUtil;


public class ChangeAttachmentChange extends Change
{
  private final Attachable m_attatchedTo;
  private final String m_attatchmentName;
  private final Object m_newValue;
  private Object m_oldValue;
  private final String m_property;
  private boolean m_clearFirst = false;

  public Attachable getAttatchedTo()
  {
    return m_attatchedTo;
  }

  public String getAttatchmentName()
  {
    return m_attatchmentName;
  }

  ChangeAttachmentChange(IAttachment attatchment, Object newValue, String property)
  {
    if(attatchment == null)
        throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
      
    m_attatchedTo = attatchment.getAttatchedTo();
    m_clearFirst = false;
    
    m_attatchmentName = attatchment.getName();
    if(property.equals("rawProperty")) {
    	String[] s = ((String) newValue).split(":");
    	m_oldValue = PropertyUtil.getRaw(s[0], attatchment);
    	m_newValue = s[1];
        m_property = s[0];
    }
    else {
    	m_oldValue = PropertyUtil.get(property, attatchment);
    	m_newValue = newValue;
        m_property = property;
    }
  }
  
  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
   */
  ChangeAttachmentChange(IAttachment attatchment, Object newValue, String property, boolean getRaw, boolean clearFirst)
  {
	    if(attatchment == null)
	        throw new IllegalArgumentException("No attachment, newValue:" + newValue + " property:" + property);
	      
	    m_attatchedTo = attatchment.getAttatchedTo();
	    m_clearFirst = clearFirst;
	    
	    m_attatchmentName = attatchment.getName();
	    if(getRaw) {
	    	m_oldValue = PropertyUtil.getRaw(property, attatchment);
	    	m_newValue = newValue;
	        m_property = property;
	    }
	    else {
	    	m_oldValue = PropertyUtil.get(property, attatchment);
	    	m_newValue = newValue;
	        m_property = property;
	    }
  }
  
  /**
   * You don't want to clear the variable first unless you are setting some variable where the setting method is actually adding things to a list rather than overwriting.
   */
  public ChangeAttachmentChange(Attachable attatchTo, String attatchmentName, Object newValue, Object oldValue, String property, boolean clearFirst)
  {
    m_attatchmentName = attatchmentName;
    m_attatchedTo = attatchTo;
    m_newValue = newValue;
    m_oldValue = oldValue;
    m_property = property;
    m_clearFirst = clearFirst;
  }


  @Override
public void perform(GameData data)
  {
      IAttachment attachment = m_attatchedTo.getAttachment(m_attatchmentName);
      PropertyUtil.set(m_property, m_newValue, attachment, m_clearFirst);
  }
  

  @Override
public Change invert()
  {
    return new ChangeAttachmentChange(m_attatchedTo, m_attatchmentName, m_oldValue, m_newValue, m_property, m_clearFirst);
  }

  @Override
public String toString()
  {
      return "ChangAttatchmentChange attatched to:" + m_attatchedTo + " name:" + m_attatchmentName + " new value:" + m_newValue + " old value:" + m_oldValue;
  }

}
