package com.oxygenxml.ditareferences.workspace;

import java.awt.event.ActionEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.StringTokenizer;

import javax.swing.AbstractAction;

import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;

import ro.sync.ecss.dita.reference.keyref.KeyInfo;
import ro.sync.exml.editor.ContentTypes;
import ro.sync.exml.workspace.api.editor.WSEditor;
import ro.sync.exml.workspace.api.standalone.StandalonePluginWorkspace;

@SuppressWarnings("serial")
public class OpenReferenceAction extends AbstractAction {
	private static final String FORMAT = "format";

	/* The OpenReferenceAction Logger. */
	private static final Logger LOGGER = Logger.getLogger(OpenReferenceAction.class);

	private NodeRange nodeRange;
	private WSEditor editorAccess;
	private StandalonePluginWorkspace pluginWorkspaceAccess;
	private KeysProvider keysProvider;

	/**
	 * Constructor for popUp menu triggered by right click.
	 * 
	 * @param nodeRange             The nodeRange to be clicked
	 * @param editorAccess          The editor access
	 * @param pluginWorkspaceAccess The pluginWorkspace access
	 * @param keysProvider          The name of the contextual menu item
	 * @param actionName            The name of the action.
	 */
	public OpenReferenceAction(NodeRange nodeRange, WSEditor editorAccess,
			StandalonePluginWorkspace pluginWorkspaceAccess, KeysProvider keysProvider, String actionName) {
		super(actionName);

		this.nodeRange = nodeRange;
		this.pluginWorkspaceAccess = pluginWorkspaceAccess;
		this.editorAccess = editorAccess;
		this.keysProvider = keysProvider;
	}

	/**
	 * Constructor when opening the reference by double click.
	 * 
	 * @param nodeRange             The nodeRange to be clicked
	 * @param editorAccess          The editor access
	 * @param pluginWorkspaceAccess The pluginWorkspace access
	 */
	public OpenReferenceAction(NodeRange nodeRange, WSEditor editorAccess,
			StandalonePluginWorkspace pluginWorkspaceAccess, KeysProvider keysProvider) {
		this(nodeRange, editorAccess, pluginWorkspaceAccess, keysProvider, "");
	}

	/**
	 * Action for open reference either by double clicking or by right clicking with
	 * contextual menu. Case for every attribute value of the leaf node. In case of
	 * key references (keyref, conkeyref) the URL location comes from the
	 * LinkedHashMap with KeyInfo.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		// attributes of the leaf nodes
		String hrefAttr = nodeRange.getAttributeValue("href");
		String keyrefAttr = nodeRange.getAttributeValue("keyref");
		String conrefAttr = nodeRange.getAttributeValue("conref");
		String conkeyrefAttr = nodeRange.getAttributeValue("conkeyref");
		String dataAttr = nodeRange.getAttributeValue("data");
		String datakeyrefAttr = nodeRange.getAttributeValue("datakeyref");
		String formatAttr = nodeRange.getAttributeValue(FORMAT);

		URL url = null;
		URL editorLocation = editorAccess.getEditorLocation();
		LinkedHashMap<String, KeyInfo> referencesKeys = keysProvider != null ? keysProvider.getKeys(editorLocation) : null;
		
		try {
			if (hrefAttr != null) {
				// possible URL if the protocol name is already inserted in the href reference
				// by user, otherwise it needs to be added
				URL possibleURL = new URL(editorLocation, hrefAttr);
				url = getURLForHTTPHost(formatAttr, hrefAttr, possibleURL);
				openReferences(url, nodeRange, formatAttr);

			} else if (conrefAttr != null) {
				url = new URL(editorLocation, conrefAttr);
				openReferences(url, nodeRange, formatAttr);

			} else if (keyrefAttr != null) {
				if (referencesKeys != null) {
					KeyInfo value = getKeyInfoFromReference(keyrefAttr, referencesKeys);
					if (value != null) {
						url = getURLForHTTPHost(formatAttr, value.getHrefValue(), value.getHrefLocation());
						formatAttr = value.getAttributes().get(FORMAT);
						openReferences(url, nodeRange, formatAttr);
					}
				}

			} else if (conkeyrefAttr != null) {
				if (referencesKeys != null) {
					KeyInfo value = getKeyInfoFromReference(conkeyrefAttr, referencesKeys);
					if (value != null) {
						url = value.getHrefLocation();
						formatAttr = value.getAttributes().get(FORMAT);
						openReferences(url, nodeRange, formatAttr);
					}
				}
			} else
				openAudioVideoReferences(dataAttr, datakeyrefAttr, formatAttr, editorLocation, referencesKeys);
			
		} catch (MalformedURLException e1) {
			LOGGER.debug(e1, e1);
		}
	}

	/**
	 * Open audio and video references.
	 * 
	 * @param dataAttr       The data attribute
	 * @param datakeyrefAttr The dataKeyRef attribute
	 * @param formatAttr     The format attribute
	 * @param editorLocation The editorLocation
	 * @param referencesKeys The LinkedHashMap of refKeys
	 * @throws MalformedURLException
	 */
	private void openAudioVideoReferences(String dataAttr, String datakeyrefAttr, String formatAttr, URL editorLocation,
			LinkedHashMap<String, KeyInfo> referencesKeys) throws MalformedURLException {
		URL url;
		if (datakeyrefAttr != null) {
			if (referencesKeys != null) {
				KeyInfo value = getKeyInfoFromReference(datakeyrefAttr, referencesKeys);
				if (value != null) {
					url = value.getHrefLocation();
					formatAttr = value.getAttributes().get(FORMAT);
					openReferences(url, nodeRange, formatAttr);
				}
			}
		} else if (dataAttr != null) {
			URL dataUrl = new URL(editorLocation, dataAttr);				
			openReferences(dataUrl, nodeRange, formatAttr);
		}
	}

	/**
	 * Get URL in case of no protocol in the attribute value. The HTTP Host should
	 * have the protocol name when new URL created. For example:
	 * "http://www.google.com" if user types "www.google.com".
	 * 
	 * @param formatAttr  Format attribute to verify
	 * @param hrefValue   The HREF value of the attribute
	 * @param possibleURL The URL if no HTML format available
	 * @return The target URL
	 * @throws MalformedURLException
	 */
	private URL getURLForHTTPHost(String formatAttr, String hrefValue, URL possibleURL) throws MalformedURLException {
		URL url;
		if (formatAttr != null && formatAttr.equals("html") && !hrefValue.startsWith("http")) {
			url = new URL("http://" + hrefValue);
		} else {
			url = possibleURL;
		}
		return url;
	}

	/**
	 * Get the specific KeyInfo of the given key after removing the "/" if any. in
	 * order to get the filename.
	 * 
	 * @param keyAttrValue The key reference attribute value
	 * @param keys         The LinkedHashMap with all the keys
	 */
	private KeyInfo getKeyInfoFromReference(String keyAttrValue, LinkedHashMap<String, KeyInfo> keys) {
		StringTokenizer st = new StringTokenizer(keyAttrValue, "/");
		String keyName = null;
		if (st.hasMoreTokens()) {
			keyName = st.nextToken();
		}
		return keys.get(keyName);
	}

	/**
	 * Open references from attribute with either Oxygen or an associated
	 * application depending on its type: images, audio / video files, DITA topic,
	 * HTML, PDF etc.
	 * 
	 * @param url                     Target URL, the URL to open
	 * @param nodeRange               The nodeRange
	 * @param referenceAttributeValue The attribute value
	 * @param formatAttr              The format attribute
	 * @throws MalformedURLException
	 * @throws DOMException
	 */
	private void openReferences(URL url, NodeRange nodeRange, String formatAttr) throws MalformedURLException {
		String classAttr = nodeRange.getAttributeValue("class");

		if (classAttr != null) {
			// it's image
			if (classAttr.contains(" topic/image ")) {
				openImageReference(url, formatAttr);
			} else
			// it's object file: audio / video
			if (classAttr.contains(" topic/object ")) {
				pluginWorkspaceAccess.openInExternalApplication(url, true);
			} else {
				if (formatAttr != null) {
					openReferenceWithFormatAttr(url, formatAttr);
				} else {
					openReferenceWithoutFormatAttr(url);
				}
			}
		}

	}

	/**
	 * Open image references from format attribute.
	 * 
	 * @param url        The target URL
	 * @param formatAttr The format attribute
	 * @throws MalformedURLException
	 */
	private void openImageReference(URL url, String formatAttr) throws MalformedURLException {
		if (pluginWorkspaceAccess.getUtilAccess().isSupportedImageURL(url)) {
			pluginWorkspaceAccess.open(url, null, ContentTypes.IMAGE_CONTENT_TYPE);
		} else {
			// image needs extension for URL if none in attributeValue
			if (formatAttr != null) {
				URL imageUrl = new URL(url.toString() + "." + formatAttr);
				if (pluginWorkspaceAccess.getUtilAccess().isSupportedImageURL(imageUrl)) {
					pluginWorkspaceAccess.open(imageUrl, null, ContentTypes.IMAGE_CONTENT_TYPE);
				}
			}
		}
	}

	/**
	 * Open reference with NO format attribute. For example, a resource opened in
	 * external application, a web link or DITA topic.
	 * 
	 * @param url        The target URL, the URL to open
	 * @param formatAttr The format Attribute
	 */
	private void openReferenceWithoutFormatAttr(URL url) {
		// binary resource or a HTML format to be opened in browser
		if (pluginWorkspaceAccess.getUtilAccess().isUnhandledBinaryResourceURL(url)) {
			pluginWorkspaceAccess.openInExternalApplication(url, true);
		} else {
			// it's DITA
			pluginWorkspaceAccess.open(url);
		}
	}

	/**
	 * Open references with format attribute. For example, DITA topic or resource
	 * opened in external application.
	 * 
	 * @param url        The target URL, the URL to open
	 * @param formatAttr The format Attribute
	 */
	private void openReferenceWithFormatAttr(URL url, String formatAttr) {
		// it's DITA
		if (formatAttr.equals("dita") || formatAttr.equals("ditamap")) {
			pluginWorkspaceAccess.open(url);
		} else {
			// it's binary resource, not handled by Oxygen
			pluginWorkspaceAccess.openInExternalApplication(url, true);
		}
	}

}
