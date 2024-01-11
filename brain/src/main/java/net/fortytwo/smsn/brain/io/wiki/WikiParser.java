package net.fortytwo.smsn.brain.io.wiki;

import net.fortytwo.smsn.SemanticSynchrony;
import net.fortytwo.smsn.brain.AtomId;
import net.fortytwo.smsn.brain.io.PageParser;
import net.fortytwo.smsn.brain.io.json.JsonFormat;
import net.fortytwo.smsn.brain.model.Property;
import net.fortytwo.smsn.brain.model.Role;
import net.fortytwo.smsn.brain.model.dto.LinkDTO;
import net.fortytwo.smsn.brain.model.dto.ListNodeDTO;
import net.fortytwo.smsn.brain.model.dto.PageDTO;
import net.fortytwo.smsn.brain.model.dto.TopicDTO;
import net.fortytwo.smsn.brain.model.dto.TreeNodeDTO;
import net.fortytwo.smsn.brain.model.entities.Link;
import net.fortytwo.smsn.brain.model.entities.ListNode;
import net.fortytwo.smsn.brain.model.entities.Page;
import net.fortytwo.smsn.brain.model.entities.Topic;
import net.fortytwo.smsn.brain.model.entities.TreeNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Stack;
import java.util.regex.Matcher;

public class WikiParser extends PageParser {

    private enum State {Properties, Content, Text}

    private Page page;
    private final Stack<Stack<TreeNode<Link>>> nodeHierarchy = new Stack<>();
    private final Stack<Integer> indentHierarachy = new Stack<>();

    private int lineNumber;
    private String currentLine;
    private String currentLineTrimmed;
    private State currentState;

    private String currentPropertyKey;
    private String currentPropertyValue;
    private Page currentPage;

    @Override
    public Page parse(final InputStream inputStream) throws IOException {
        return parseInternal(inputStream);
    }

    private Page parseInternal(final InputStream inputStream) throws IOException {
        reset();

        BufferedReader br = createReader(inputStream);
        while ((currentLine = br.readLine()) != null) {
            parseLine();
        }

        adjustHierarchy(-1);

        return page;
    }

    private Page createPage() {
        // TODO: supply id externally
        Topic topic = new TopicDTO();
        topic.setId(null);
        Link link = new LinkDTO();
        // TODO: set label externally
        link.setLabel(null);
        link.setTarget(topic);
        TreeNode<Link> content = new TreeNodeDTO<>();
        content.setValue(link);

        Page page = new PageDTO();
        page.setContent(content);
        // TODO: set source externally

        return page;
    }

    private Page copyPage() {
        Page copy = new PageDTO();

        for (Property prop : Page.propertiesByKey.values()) {
            prop.getSetter().accept(copy, prop.getGetter().apply(page));
        }

        return copy;
    }

    private void parseLine() throws IOException {
        incrementLineNumber();

        replaceTabsWithSpaces();

        currentLineTrimmed = currentLine.trim();

        switch (currentState) {
            case Properties:
                if (!currentLineIsEmpty()) {
                    if (currentLineIsProperty()) {
                        parsePropertyLine(page);
                        break;
                    } else {
                        // note: falls through to "Content" case
                        currentState = State.Content;
                    }
                }
            case Content:
                if (!currentLineIsEmpty()) {
                    validateContentLine();
                    parseContentLine();
                }
                break;
            case Text:
                if (currentLineTrimmed.equals(WikiFormat.MULTILINE_DELIMITER)) {
                    currentState = State.Properties;
                    finishProperty();
                } else {
                    if (!currentPropertyValue.isEmpty()) currentPropertyValue += "\n";
                    currentPropertyValue += currentLine;
                }
                break;
        }
    }

    private BufferedReader createReader(final InputStream in) throws IOException {
        return new BufferedReader(new InputStreamReader(in, SemanticSynchrony.UTF8));
    }

    private void reset() {
        nodeHierarchy.clear();
        indentHierarachy.clear();
        indentHierarachy.push(-1);

        currentState = State.Properties;
        lineNumber = 0;

        page = createPage();
        nodeHierarchy.push(new Stack<>());
        nodeHierarchy.peek().push(page.getContent());
    }

    private void replaceTabsWithSpaces() {
        // Tabs count as four spaces each.
        currentLine = currentLine.replaceAll("[\\t]", WikiFormat.TAB_REPLACEMENT);
    }

    private void validateContentLine() throws IOException {
        if (currentLine.endsWith(JsonFormat.TITLE_TRUNCATOR)) {
            parseError("line ends with the reserved truncation sequence \"" + JsonFormat.TITLE_TRUNCATOR + "\"");
        }
    }

    private int findIndentLevel() {
        int level = 0;
        if (null != currentLine && currentLine.length() > 0) {
            while (' ' == currentLine.charAt(level)) {
                level++;
            }
        }
        return level;
    }

    private void adjustHierarchy(final int indentLevel) {
        while (indentLevel < indentHierarachy.peek()) {
            indentHierarachy.pop();
            Stack<TreeNode<Link>> siblings = nodeHierarchy.pop();
            TreeNode<Link> parent = nodeHierarchy.peek().peek();
            addChildren(parent, siblings);
        }

        if (indentLevel > indentHierarachy.peek()) {
            indentHierarachy.push(indentLevel);
            nodeHierarchy.push(new Stack<>());
        }
    }

    private TreeNode<Link> getCurrentNode() {
        return nodeHierarchy.peek().peek();
    }

    private void addToHierarchy(final TreeNode<Link> tree) {
        adjustHierarchy(findIndentLevel());

        nodeHierarchy.peek().push(tree);
    }

    private void addChildren(final TreeNode<Link> parent, Stack<TreeNode<Link>> children) {
        if (children.isEmpty()) {
            return;
        }

        ListNode<TreeNode<Link>> cur = null;
        while (!children.isEmpty()) {
            cur = new ListNodeDTO<>(children.pop(), cur);
        }
        parent.setChildren(cur);
    }

    private boolean currentLineIsEmpty() {
        return currentLineTrimmed.isEmpty();
    }

    private boolean currentLineIsProperty() {
        return currentLineTrimmed.startsWith("@");
    }

    private void validateLink(Link link) throws IOException {
        if (null != link.getLabel() && 0 == link.getLabel().length()) {
            if (null == link.getTarget()) {
                parseError("empty label in placeholder link");
            } else {
                // Empty labels are allowed for existing links.
                // They signify that an existing link's label should not be overwritten.
                link.setLabel(null);
            }
        }
    }

    private void parseError(final String message) throws IOException {
        throw new IOException("line " + lineNumber + ": " + message);
    }

    private void parsePropertyLine(final Page page) throws IOException {
        currentPage = page;

        int firstSpace = currentLineTrimmed.indexOf(' ');
        if (-1 == firstSpace) {
            currentPropertyKey = currentLineTrimmed.substring(1);
            currentPropertyValue = "";
        } else if (firstSpace < 2) {
            parseError("empty property key");
        } else {
            currentPropertyKey = currentLineTrimmed.substring(1, firstSpace).trim();
            String value = currentLineTrimmed.substring(firstSpace).trim();
            if (value.equals(WikiFormat.MULTILINE_DELIMITER)) {
                currentPropertyValue = "";
                currentState = State.Text;
                return;
            } else {
                currentPropertyValue = value;
            }
        }

        finishProperty();
    }

    private void finishProperty() throws IOException {
        String key = currentPropertyKey;
        String value = WikiFormat.stripTrailingSpace(currentPropertyValue);

        checkForEmptyPropertyValue(key, value);
        setProperty(currentPage, key, value);
    }

    private void checkForEmptyPropertyValue(final String key, final String value) throws IOException {
        if (value.isEmpty()) {
            // can "clear" alias or shortcut by writing "@alias" or "@shortcut" and nothing else;
            // all other properties require an argument
            if (!(key.equals("alias") || key.equals("shortcut") || key.equals("text"))) {
                parseError("empty value for property @" + key);
            }
        }
    }

    private void parseContentLine() throws IOException {
        if (currentLineTrimmed.startsWith("@")) {
            parseContentPropertyLine();
        } else {
            parseContentTitleLine();
        }
    }

    private void parseContentPropertyLine() throws IOException {
        parsePropertyLine(getCurrentNode().getValue().getPage());
    }

    private void parseContentTitleLine() throws IOException {
        int firstSpace = currentLineTrimmed.indexOf(' ');
        if (-1 == firstSpace) {
            parseError("missing item bullet");
        } else if (firstSpace > 2) {
            parseError("bullet is too long");
        }

        String bullet = currentLineTrimmed.substring(0, firstSpace);
        String rest = currentLineTrimmed.substring(firstSpace).trim();

        AtomId id;
        String label;
        Matcher matcher = WikiFormat.ID_INFIX.matcher(rest);
        if (matcher.find() && 0 == matcher.start()) {
            id = new AtomId(rest.substring(1, matcher.end() - 1));
            label = rest.substring(matcher.end()).trim();
        } else {
            id = null;
            label = rest;
        }

        TreeNode<Link> tree = constructTreeNode(id, tagForBullet(bullet), label);
        addToHierarchy(tree);
    }

    private <T> void setProperty(final Page page, final String key, String value) throws IOException {
        if (value.length() == 0) {
            value = WikiFormat.CLEARME;
        }

        // TODO: transitional
        switch (key) {
            case "id":
                page.getContent().getValue().getTarget().setId(new AtomId(value));
                break;
            case "title":
                page.getContent().getValue().setLabel(value);
                break;
            default:
                Property<Page, T> prop = (Property<Page, T>) Page.propertiesByKey.get(key);
                if (null == prop) {
                    // unknown properties are quietly ignored
                    return;
                }

                T typeSafeValue;

                try {
                    typeSafeValue = prop.getFromString().apply(value);
                } catch (Exception e) {
                    parseError("invalid value for @" + key + " property: " + value);
                    return;
                }

                prop.getSetter().accept(page, typeSafeValue);
                break;
        }
    }

    private Role tagForBullet(final String bullet) {
        return bullet.equals(WikiFormat.LABEL_BULLET)
                ? Role.Relation
                : null;
    }

    private TreeNode<Link> constructTreeNode(final AtomId topicId, final Role role, final String label)
            throws IOException {
        Link link = new LinkDTO();
        link.setRole(role);
        link.setLabel(label);
        if (null != topicId) {
            Topic target = new TopicDTO();
            if (null != topicId) {
                target.setId(topicId);
            }
            link.setTarget(target);
        }
        //link.setPage(page);
        link.setPage(copyPage());

        validateLink(link);

        TreeNode<Link> treeNode = new TreeNodeDTO<>();
        treeNode.setValue(link);
        return treeNode;
    }

    private void incrementLineNumber() {
        lineNumber++;
    }
}
