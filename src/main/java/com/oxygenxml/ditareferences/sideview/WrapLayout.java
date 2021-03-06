package com.oxygenxml.ditareferences.sideview;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 *  FlowLayout subclass that fully supports wrapping of components.
 */
public class WrapLayout extends FlowLayout {
  
  /**
   * The ID
   */
  private static final long serialVersionUID = 1L;

  /**
   * On layout, we detected that the allocated height is not enough for all the components to fit.
   * When this happens, we request more height from an ancestor with enough height.
   */
  private Dimension lastBadSizeRefreshRequest;

  /**
  * Constructs a new <code>WrapLayout</code> with a left
  * alignment and a default 5-unit horizontal and vertical gap.
  */
  public WrapLayout() {
    super();
  }

  /**
   * Constructs a new <code>FlowLayout</code> with the specified alignment and a default 5-unit 
   * horizontal and vertical gap.
   * The value of the alignment argument must be one of <code>WrapLayout.LEFT</code>, 
   * <code>WrapLayout.RIGHT</code>, <code>WrapLayout.CENTER</code>, <code>WrapLayout.LEADING</code>,
   * or <code>WrapLayout.TRAILING</code>.
   * 
   * @param align the alignment value
   */
  public WrapLayout(int align) {
    super(align);
  }

  /**
   * Creates a new flow layout manager with the indicated alignment and the indicated horizontal and vertical gaps.
   * <p>
   * The value of the alignment argument must be one of <code>WrapLayout.LEFT</code>, 
   * <code>WrapLayout.RIGHT</code>, <code>WrapLayout.CENTER</code>, <code>WrapLayout.LEADING</code>,
   * or <code>WrapLayout.TRAILING</code>.
   * 
   * @param align the alignment value
   * @param hgap the horizontal gap between components
   * @param vgap the vertical gap between components
   */
  public WrapLayout(int align, int hgap, int vgap) {
    super(align, hgap, vgap);
  }

  /**
   * Returns the preferred dimensions for this layout given the <i>visible</i> components 
   * in the specified target container.
   * @param target the component which needs to be laid out
   * @return the preferred dimensions to lay out the subcomponents of the specified container
   */
  @Override
  public Dimension preferredLayoutSize(Container target) {
    return layoutSize(target, true);
  }

  /**
  * Returns the minimum dimensions needed to layout the <i>visible</i> components contained 
  * in the specified target container.
  * @param target the component which needs to be laid out
  * @return the minimum dimensions to lay out the subcomponents of the specified container
  */
  @Override
  public Dimension minimumLayoutSize(Container target) {
    Dimension minimum = layoutSize(target, false);
    minimum.width -= (getHgap() + 1);
    return minimum;
  }

  /**
  * Returns the minimum or preferred dimension needed to layout the target
  * container.
  *
  * @param container target to get layout size for
  * @param preferred should preferred size be calculated
  * @return the dimension to layout the target container
  */
  private Dimension layoutSize(Container container, boolean preferred) {
    synchronized (container.getTreeLock()) {
      //  Each row must fit with the width allocated to the container.
      //  When the container width = 0, the preferred width of the container
      //  has not yet been calculated so lets ask for the maximum.
      int containerWidth = container.getSize().width;
      if (containerWidth == 0) {
        containerWidth = Integer.MAX_VALUE;
      }

      // Compute the container maximum width 
      int containerHorizontalGap = getHgap();
      int containerVerticalGap = getVgap();
      Insets insets = container.getInsets();
      int horizontalInsetsAndGap = insets.left + insets.right + (containerHorizontalGap * 2);
      int maxWidth = containerWidth - horizontalInsetsAndGap;

      //  Fit components into the allowed width
      Dimension containerDim = new Dimension(0, 0);
      int rowWidth = 0;
      int rowHeight = 0;

      int componentCount = container.getComponentCount();

      for (int i = 0; i < componentCount; i++) {
        Component component = container.getComponent(i);

        if (component.isVisible()) {
          // Get current component dimension
          Dimension compDimension = preferred ? component.getPreferredSize() : component.getMinimumSize();

          //  Can't add the component to current row. Start a new row.
          if (rowWidth + compDimension.width > maxWidth) {
            addNewRow(containerDim, rowWidth, rowHeight);
            rowWidth = 0;
            rowHeight = 0;
          }

          //  Add a horizontal gap for all components after the first
          if (rowWidth != 0) {
            rowWidth += containerHorizontalGap;
          }

          // Append the current component width
          rowWidth += compDimension.width;
          // Update the row height if necessary
          rowHeight = Math.max(rowHeight, compDimension.height);
        }
      }

      // Last row was added, update the preferred size of the container
      addNewRow(containerDim, rowWidth, rowHeight);

      // Append insets and horizontal gap to the container width
      containerDim.width += horizontalInsetsAndGap;
      // Append insets and vertical gap to the container height
      containerDim.height += insets.top + insets.bottom + containerVerticalGap * 2;

      //	When using a scroll pane or the DecoratedLookAndFeel we need to
      //  make sure the preferred size is less than the size of the
      //  target container so shrinking the container size works
      //  correctly. Removing the horizontal gap is an easy way to do this.
      Container scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, container);
      if (scrollPane != null) {
        containerDim.width -= (containerHorizontalGap + 1);
      }

      return containerDim;
    }
  }
  
  /**
   * @see java.awt.FlowLayout#layoutContainer(java.awt.Container)
   */
  @Override
  public void layoutContainer(Container target) {
    super.layoutContainer(target);
    
    ensureRequiredHeight(target);
  }

  /**
   * Checks if all the components fit inside the allocated height. If they don't, request a relayout 
   * from the ancestors just in case they can give more height.
   * 
   * When you shrink a container (a resize), only the children of the top-most container get a size update before 
   * computing the preferred size down the hierarchy. The descendants still have the previous size so layoutSize() 
   * makes the layout thinking that it has more space than it will actually have. 
   * 
   * @param target The container associated with the layout.
   */
  private void ensureRequiredHeight(Container target) {
    Dimension size = target.getSize();

    int neededHeight = -1;
    boolean outside = false;
    int noOfTargetComponents = target.getComponentCount();
    if (noOfTargetComponents > 0) {
      // It is enough to check the last component and see if it fits in the allocated height.
      Component comp = target.getComponent(noOfTargetComponents - 1);
      Rectangle bounds = comp.getBounds();
      neededHeight = bounds.y + bounds.height;
      if (neededHeight > size.height) {
        outside = true;
      }
    }
    
    // Avoid requesting more height if a previous attempt was unsuccessful.
    if (outside && !size.equals(lastBadSizeRefreshRequest)) {

      // Remember the last size for which we requested more height.
      lastBadSizeRefreshRequest = size;
      List<Container> parents = new LinkedList<>();
      Container parent = target.getParent();

      while (parent != null) {
        parents.add(parent);
        if (parent.getSize().height > neededHeight) {
          SwingUtilities.invokeLater(() -> {
            for (Container container : parents) {
              container.invalidate();
              container.revalidate();
            }
          });
          break;
        }
        parent = parent.getParent();
      }
    } else {
      // The current size is enough. Reset the flag.
      lastBadSizeRefreshRequest = null;
    }
  }

  /**
   *  A new row has been completed. Use the dimensions of this row to update the preferred size for the container.
   *
   *  @param dim update the width and height when appropriate
   *  @param rowWidth the width of the row to add
   *  @param rowHeight the height of the row to add
   */
  private void addNewRow(Dimension dim, int rowWidth, int rowHeight) {
    dim.width = Math.max(dim.width, rowWidth);
    if (dim.height > 0) {
      dim.height += getVgap();
    }
    dim.height += rowHeight;
  }
}
