package msharp.Nodes;

import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import msharp.NotePopulation.BuildNoteListVisitor;
import msharp.NotePopulation.FinalNote;
import msharp.NotePopulation.NodeContext;

import java.util.List;
import java.util.UUID;

import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;

public class RepeatNode implements StmtNode {
    private final int amount;
    
    private final StmtNode stmts;
    
    public StmtNode getStmts ()
    {
        return stmts;
    }
    
    public RepeatNode (int amount, StmtNode stmts)
    {
        this.amount = amount;
        this.stmts = stmts;
    }
    
    @Override
    public String toString () { return "REPEAT (" + amount + ") {" + stmts.toString() + "}"; }
    
    @Override
    public Graph toGraph ()
    {
        Node repeat = node("repeat" + UUID.randomUUID().toString()).with(Color.RED).with(Label.html("<b>REPEAT</b><br/>" + amount + "times"));
        
        Graph g = graph("repeat").directed().graphAttr().with(Rank.dir(TOP_TO_BOTTOM));
        g = g.with(repeat.link(stmts.toGraph().toMutable().rootNodes().iterator().next()));
        
        return g;
    }
    
    @Override
    public List<FinalNote> accept (BuildNoteListVisitor visitor, NodeContext ctx)
    {
        return visitor.visit(this, ctx);
    }
    
    public int getAmount ()
    {
        return amount;
    }
}
