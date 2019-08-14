package com.oxygenxml.sdksamples.workspace.authorpage;

import java.util.ArrayList;
import java.util.List;

import com.oxygenxml.sdksamples.workspace.NodeRange;
import com.oxygenxml.sdksamples.workspace.ReferencesCollector;

import ro.sync.ecss.extensions.api.AuthorOperationException;
import ro.sync.ecss.extensions.api.node.AuthorElement;
import ro.sync.ecss.extensions.api.node.AuthorNode;
import ro.sync.exml.workspace.api.editor.page.WSEditorPage;
import ro.sync.exml.workspace.api.editor.page.author.WSAuthorEditorPage;
import ro.sync.exml.workspace.api.editor.page.text.xml.XPathException;

/**
 * Evaluate the XPath expression and get the Author NodeRanges.
 * 
 * @author Alexandra_Dinisor
 *
 */
public class AuthorReferencesCollector extends ReferencesCollector {
	@Override
	protected List<NodeRange> collect(WSEditorPage page) throws XPathException {
		WSAuthorEditorPage authorEditorPage = (WSAuthorEditorPage) page;
		List<NodeRange> ranges = new ArrayList<NodeRange>();

		try {
			AuthorNode[] referenceAuthorNodeRanges = authorEditorPage.getDocumentController()
					.findNodesByXPath(ALL_REFS_XPATH_EXPRESSION, false, false, false);

			// add all Author NodeRanges found by XPath evaluation
			for (int i = 0; i < referenceAuthorNodeRanges.length; i++) {	
//				System.err.println("__________________________________________");
//				System.err.println(
//						"getDisplayName(): " + referenceAuthorNodeRanges[i].getDisplayName() + " | getStartOffset(): "
//								+ referenceAuthorNodeRanges[i].getStartOffset() + " | getEndOffset(): "
//								+ referenceAuthorNodeRanges[i].getEndOffset());
//				System.err.println(" ---> getAttributes: ");
//				for (int j = 0; j < ((AuthorElement) referenceAuthorNodeRanges[i]).getAttributesCount(); j++) {
//					String attributeName = ((AuthorElement) referenceAuthorNodeRanges[i]).getAttributeAtIndex(j);
//					System.err.println("attributeName: " +  attributeName
//							+ " | getValue(): " + ((AuthorElement) referenceAuthorNodeRanges[i]).getAttribute(attributeName).getValue());
//				}

				NodeRange refRange = new AuthorPageNodeRange((AuthorElement) referenceAuthorNodeRanges[i]);
				ranges.add(refRange);
			}
		} catch (AuthorOperationException e) {
			throw new XPathException(e.getMessage(), e);
		}

		return ranges;
	}
}