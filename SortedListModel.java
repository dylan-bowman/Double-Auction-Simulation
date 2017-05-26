/* Code used from http://www.jguru.com/faq/view.jsp?EID=15245
 * created by John Zukowski.
 * 
 * */

import javax.swing.*;
import java.util.*;

@SuppressWarnings("serial")
public class SortedListModel extends AbstractListModel {
	
	private SortedSet<LimitOrder> model;
	
	public SortedListModel(boolean opposite) {
		if (opposite) {
			model = new TreeSet<LimitOrder>(Collections.reverseOrder());
		} else {
			model = new TreeSet<LimitOrder>();
		}
	}
	
// ListModel methods
  public int getSize() {
    // Return the model size
    return model.size();
  }

  public Object getElementAt(int index) {
    // Return the appropriate element
    return model.toArray()[index];
  }

  // Other methods
  public void add(LimitOrder element) {
    if (model.add(element)) {
      fireContentsChanged(this, 0, getSize());
    }
  }

  public void addAll(LimitOrder elements[]) {
    Collection<LimitOrder> c = Arrays.asList(elements);
    model.addAll(c);
    fireContentsChanged(this, 0, getSize());
  }

  public void clear() {
    model.clear();
    fireContentsChanged(this, 0, getSize());
  }

  public boolean contains(Object element) {
    return model.contains(element);
  }

  public Object firstElement() {
    // Return the appropriate element
    return model.first();
  }

  public Iterator<LimitOrder> iterator() {
    return model.iterator();
  }

  public Object lastElement() {
    // Return the appropriate element
    return model.last();
  }

  public boolean removeElement(Object element) {
    boolean removed = model.remove(element);
    if (removed) {
      fireContentsChanged(this, 0, getSize());
    }
    return removed;   
  }
	
}
