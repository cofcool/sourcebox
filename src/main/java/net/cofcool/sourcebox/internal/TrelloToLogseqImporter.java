package net.cofcool.sourcebox.internal;

import java.io.FileReader;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import net.cofcool.sourcebox.Tool;
import net.cofcool.sourcebox.ToolName;
import net.cofcool.sourcebox.internal.trello.ActionsItem;
import net.cofcool.sourcebox.internal.trello.CardsItem;
import net.cofcool.sourcebox.internal.trello.CheckItemsItem;
import net.cofcool.sourcebox.internal.trello.ChecklistsItem;
import net.cofcool.sourcebox.internal.trello.LabelsItem;
import net.cofcool.sourcebox.internal.trello.ListsItem;
import net.cofcool.sourcebox.internal.trello.Trello;
import net.cofcool.sourcebox.util.JsonUtil;
import net.cofcool.sourcebox.util.LogseqOutStr;

public class TrelloToLogseqImporter implements Tool {


    @Override
    public ToolName name() {
        return ToolName.trelloLogseqImporter;
    }

    @Override
    public void run(Args args) throws Exception {
        var path = args.readArg("path").val();
        var outPath = args.readArg("out");
        var titleToPage = args.readArg("titleToPage").getVal().orElse("false").equalsIgnoreCase("true");
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

                        LocalDateTime time = LocalDateTime.parse(date, LogseqOutStr.FORMATTER);
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
            .alias("trello", name(), "path", null)
            .runnerTypes(EnumSet.of(RunnerType.CLI, RunnerType.WEB));
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
