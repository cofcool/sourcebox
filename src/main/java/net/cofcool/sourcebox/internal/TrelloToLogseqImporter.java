package net.cofcool.sourcebox.internal;

import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.api.entity.ActionRecord;
import net.cofcool.sourcebox.internal.api.entity.ActionType.Type;
import net.cofcool.sourcebox.internal.api.entity.CheckList;
import net.cofcool.sourcebox.internal.api.entity.Comment;
import net.cofcool.sourcebox.internal.trello.*;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.LogseqOutStr;
import net.cofcool.sourcebox.util.Utils;

import java.io.FileReader;
import java.net.http.HttpRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.cofcool.sourcebox.util.LogseqOutStr.FORMATTER;

public class TrelloToLogseqImporter implements Tool {


    @Override
    public ToolName name() {
        return ToolName.trelloLogseqImporter;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("path").val();
        var outPath = args.readArg("out");
        var titleToPage = args.readArg("titleToPage").optVal().orElse("false").equalsIgnoreCase("true");
        var importRecords = args.readArg("importRecords");
        try (var reader = new FileReader(path)) {
            Trello trello = JsonUtil.getObjectMapper().readValue(reader, Trello.class);
            var name = trello.name();
            for (Map.Entry<CreateTime, List<CardsItem>> entry : trello.cards()
                    .stream()
                    .collect(Collectors.groupingBy(a -> {
                        Optional<ActionsItem> cardDate = cardDate(trello, a.id());
                        String date;
                        if (cardDate.isPresent()) {
                            date = cardDate.get().date();
                        } else {
                            date = actionList(trello, a.id())
                                .stream()
                                .findFirst()
                                .map(i -> i.date())
                                .orElse(a.dateLastActivity());
                        }

                        LocalDateTime time = LocalDateTime.parse(date, FORMATTER);
                        return new CreateTime(time.getYear(), time);
                    }))
                    .entrySet()) {
                var out = new LogseqOutStr();
                for (CardsItem card : entry.getValue()) {
                    var cardBoard = cardList(trello, card.idList());
                    out.block(LogseqOutStr.cardTask(cardBoard.name()), 0)
                        .blockRef(titleToPage ? card.name() : null)
                        .append(titleToPage ? null : card.name())
                        .blockRef(LogseqOutStr.DATE_FORMATTER.format(entry.getKey().time()))
                        .tag(card.badges().votes() > 0 ? "recommend" : "")
                        .tag(cardBoard.name().replace(" ", "-"))
                        .tag(name);
                    for (LabelsItem label : card.labels()) {
                        out.tag(label.name().replace(" ", "-"));
                    }
                    out.breakLine();
                    if (!card.desc().isBlank()) {
                        out.block(card.desc(), 1).breakLine();
                    }

                    for (String id : card.idChecklists()) {
                        var checklist = checkItemsList(trello, id);
                        out.block(checklist.name(), 1).breakLine();
                        for (CheckItemsItem item : checklist.checkItems()) {
                            out.block(LogseqOutStr.sateTask(item.state()) + item.name(), 2).breakLine();
                        }
                    }

                    for (ActionsItem actionsItem : actionList(trello, card.id())) {
                        String text = actionsItem.data().text();
                        if (text != null && !text.isBlank()) {
                            out.block(text, 1).blockRef(LogseqOutStr.date(actionsItem.date())).breakLine();
                        }
                    }
                    if (importRecords.isPresent()) {
                        importRecord(trello, cardBoard, card, name, entry.getKey().time(), importRecords.val());
                    }
                }
                String output = outPath.val() + "/" + "trello" + name + "-" + entry.getKey().year() + ".md";
                args.getContext().write(output, out.toString());
                getLogger().info("Generate " + output + " ok");
            }
        }

    }

    @Override
    public Args config() {
        return new Args()
            .arg(new Arg("path", null, "trello json file path", true, "./demo.json"))
            .arg(new Arg("out", "./trello", "output directory", false, null))
            .arg(new Arg("titleToPage", "false", "make card title to page", false, null))
            .arg(new Arg("importRecords", null, "import cards and checklists as hisRecord data", false, "http://localhost:38080"))
            .alias("trello", name(), "path", null)
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
    }

    private void importRecord(Trello trello, ListsItem cardBoard, CardsItem card, String category, LocalDateTime createdAt, String url) {
        var actionId = Utils.md5(card.name() + Type.record.name());
        var labels = card.labels().stream().map(LabelsItem::name).collect(Collectors.joining(","));
        var record = ActionRecord.builder()
            .id(actionId)
            .name(card.name())
            .type(Type.record.name())
            .state(LogseqOutStr.cardTask(cardBoard.name()))
            .category(category)
            .start(createdAt)
            .end(createdAt)
            .labels(labels.isEmpty() ?cardBoard.name() : String.join(",", labels, cardBoard.name()))
            .remark(card.desc())
            .build();
        postRequestLocalData(url, "/action?type=record", record, ActionRecord.class);

        for (String checklistId : card.idChecklists()) {
            var checklist = checkItemsList(trello, checklistId);
            for (CheckItemsItem item : checklist.checkItems()) {
                postRequestLocalData(url, "/action/checklist/" + actionId,
                    toCheckList(item, actionId),
                    CheckList.class);
            }
        }

        for (ActionsItem action : actionList(trello, card.id())) {
            String text = action.data().text();
            if (text != null && !text.isBlank()) {
                postRequestLocalData(url, "/action/comment/" + actionId,
                    Comment.builder().content(text).createTime(LocalDateTime.parse(action.date(), FORMATTER)).build(),
                    Comment.class);
            }
        }
    }

    private <T> T postRequestLocalData(String url, String methodPath, Object params, Class<T> bodyType) {
        return Utils.requestAPI(url, methodPath, bodyType, b -> {
                b.POST(HttpRequest.BodyPublishers.ofString(JsonUtil.toJson(params)));
                return b;
            }, e -> {
                if (e != null) {
                    throw new IllegalStateException("request " + methodPath + " error", e);
                }
            });
    }

    static CheckList toCheckList(CheckItemsItem item, String actionId) {
        boolean complete = "complete".equalsIgnoreCase(item.state());
        return CheckList.builder()
            .actionId(actionId)
            .content(item.name())
            .first(0)
            .last(1)
            .current(complete ? 1 : 0)
            .build();
    }

    private List<ActionsItem> actionList(Trello trello, String id) {
        return trello
            .actions()
            .stream()
            .filter(Objects::nonNull).filter(a -> a.data().card() != null)
            .filter(a -> a.data().card().id().equals(id))
            .sorted(Comparator.comparing(ActionsItem::date))
            .toList();
    }

    private ListsItem cardList(Trello trello, String id) {
        return trello.lists().stream().filter(a -> a.id().equals(id)).findAny().get();
    }

    private Optional<ActionsItem> cardDate(Trello trello, String id) {
        return trello.actions().stream().filter(a -> a.type().equals("createCard")).filter(a -> a.data().card().id().equals(id)).findAny();
    }

    private ChecklistsItem checkItemsList(Trello trello, String id) {
        return trello.checklists().stream().filter(a -> a.id().equals(id)).findAny().get();
    }

    record CreateTime(long year, LocalDateTime time) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CreateTime that = (CreateTime) o;
            return year == that.year;
        }

        @Override
        public int hashCode() {
            return Objects.hash(year);
        }
    }
}
