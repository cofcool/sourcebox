package net.cofcool.sourcebox.internal;

import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Expander;
import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

public class FakeLineReader implements LineReader {
    private final Queue<String> inputs = new LinkedList<>();

    public void addInput(String input) {
        inputs.add(input);
    }

    @Override
    public String readLine(String prompt) {
        return inputs.poll();
    }

    @Override public String readLine(String prompt, Character mask) { throw new UnsupportedOperationException(); }

    @Override
    public String readLine(String prompt, Character mask, String buffer)
        throws UserInterruptException, EndOfFileException {
        return "";
    }

    @Override
    public String readLine(String prompt, String rightPrompt, Character mask, String buffer)
        throws UserInterruptException, EndOfFileException {
        return "";
    }

    @Override
    public String readLine(String prompt, String rightPrompt, MaskingCallback maskingCallback,
        String buffer) throws UserInterruptException, EndOfFileException {
        return "";
    }

    @Override
    public void printAbove(String str) {

    }

    @Override
    public void printAbove(AttributedString str) {

    }

    @Override
    public boolean isReading() {
        return false;
    }

    @Override
    public LineReader variable(String name, Object value) {
        return null;
    }

    @Override
    public LineReader option(Option option, boolean value) {
        return null;
    }

    @Override
    public void callWidget(String name) {

    }

    @Override
    public Map<String, Object> getVariables() {
        return Map.of();
    }

    @Override
    public Object getVariable(String name) {
        return null;
    }

    @Override
    public void setVariable(String name, Object value) {

    }

    @Override
    public boolean isSet(Option option) {
        return false;
    }

    @Override
    public void setOpt(Option option) {

    }

    @Override
    public void unsetOpt(Option option) {

    }

    @Override
    public Terminal getTerminal() {
        return null;
    }

    @Override
    public Map<String, Widget> getWidgets() {
        return Map.of();
    }

    @Override
    public Map<String, Widget> getBuiltinWidgets() {
        return Map.of();
    }

    @Override
    public Buffer getBuffer() {
        return null;
    }

    @Override
    public String getAppName() {
        return "";
    }

    @Override
    public void runMacro(String macro) {

    }

    @Override
    public MouseEvent readMouseEvent() {
        return null;
    }

    @Override
    public History getHistory() {
        return null;
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public Highlighter getHighlighter() {
        return null;
    }

    @Override
    public Expander getExpander() {
        return null;
    }

    @Override
    public Map<String, KeyMap<Binding>> getKeyMaps() {
        return Map.of();
    }

    @Override
    public String getKeyMap() {
        return "";
    }

    @Override
    public boolean setKeyMap(String name) {
        return false;
    }

    @Override
    public KeyMap<Binding> getKeys() {
        return null;
    }

    @Override
    public ParsedLine getParsedLine() {
        return null;
    }

    @Override
    public String getSearchTerm() {
        return "";
    }

    @Override
    public RegionType getRegionActive() {
        return null;
    }

    @Override
    public int getRegionMark() {
        return 0;
    }

    @Override
    public void addCommandsInBuffer(Collection<String> commands) {

    }

    @Override
    public void editAndAddInBuffer(Path file) throws Exception {

    }

    @Override
    public String getLastBinding() {
        return "";
    }

    @Override
    public String getTailTip() {
        return "";
    }

    @Override
    public void setTailTip(String tailTip) {

    }

    @Override
    public void setAutosuggestion(SuggestionType type) {

    }

    @Override
    public SuggestionType getAutosuggestion() {
        return null;
    }

    @Override
    public void zeroOut() {

    }

    @Override
    public Map<String, KeyMap<Binding>> defaultKeyMaps() {
        return Map.of();
    }

    @Override public String readLine() { throw new UnsupportedOperationException(); }

    @Override
    public String readLine(Character mask) throws UserInterruptException, EndOfFileException {
        return "";
    }

    // ...省略未用的方法（或用默认实现）
}
