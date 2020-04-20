package msharp.NotePopulation;

import msharp.MinecraftClasses.Instrument;
import msharp.Nodes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuildNoteListVisitor {
    // todo Set standard octave, hvis nu den første Node ikke har en oktav
    private Map<String, Object> symbolTable = new HashMap<>();

    public BuildNoteListVisitor(Map<String, Object> symbolTable) {
        this.symbolTable = symbolTable;
    }

    public List<FinalNote> visit(ProgNode prog){
        // ctx is used to store current context about variables such as octaves, instrument, Bpm, Tempo and timing.
        NodeContext ctx =  new NodeContext();
        return visit(prog.main,ctx);
    }

    private List<FinalNote> visit(PlayNode play, NodeContext ctx) {
        List<FinalNote> noteList = new ArrayList<>();
        for(StmtNode stmt : play.stmts){
            noteList.addAll(stmt.accept(this,ctx));
        }
        // De foskellige muligheder for stmt notes
        // 1. Noget der ændrer i ctx
        // 2. Noget der tilføjer x antal noter
        //
        return noteList;
    }

    public List<FinalNote> visit(AndNode node, NodeContext ctx){
        List<FinalNote> notes = new ArrayList<>();

        // Makes a clone of the current context, so the changes to the context doesn't overlap into the "global" scope
        NodeContext leftCtx = ctx.clone();
        notes.addAll(node.getLeft().accept(this,leftCtx));

        NodeContext rightCtx = ctx.clone();
        notes.addAll(node.getRight().accept(this,rightCtx));

        // we now set the timing to the max point of either left or right.
        // todo var det den her måde timing skulle virke på????
        ctx.timing = leftCtx.timing;
        return notes;
    }

    public List<FinalNote> visit(BpmDclNode node, NodeContext ctx){
        // This visit should only change the ctx, and add no new notes.
        ctx.bpm.bpm = node.getBpm();
        ctx.bpm.tempo = new Tempo(node.getTempo().getNumerator(),node.getTempo().getDenominator());
        return new ArrayList<>();
    }

    public List<FinalNote> visit(EveryNode node, NodeContext ctx){
        List<FinalNote> notes = new ArrayList<>();

        // Integer on the stack is 1-based
        if(ctx.repeatIterationStack.peek().value % node.getAmount() == 0){
            notes.addAll(node.getTrueCase().accept(this,ctx));
        }
        else{
            notes.addAll(node.getElseCase().accept(this,ctx));
        }

        return notes;
    }

    public List<FinalNote> visit(IdNode node, NodeContext ctx){

        if(!symbolTable.containsKey(node.getId())){
            // todo throw some error?
            throw new IllegalArgumentException("Id does not exists in symboltable"+node.getId());
        }

        return ((StmtList)symbolTable.get(node.getId())).accept(this,ctx);
    }

    public List<FinalNote> visit(InstruNode node, NodeContext ctx){
        // This visit should only change the ctx, and add no new notes.
        ctx.instrument = Instrument.fromString(node.getInstrument());
        return new ArrayList<>();
    }

    public List<FinalNote> visit(NoteNode node, NodeContext ctx){
        List<FinalNote> notes = new ArrayList<>();

        // if NoteNode has an octave defined, we change the octave defined in the current ctx.
        if(node.getOctave() != -1){
            ctx.octave = node.getOctave();
        }

        // if the Node isn't a pause, we add the Node to the Node list.
        if(node.getLetter() != '-') {
            notes.add(new FinalNote(ctx.instrument, ToneEnum.fromLetter(node.getLetter()), ctx.octave, ctx.timing));
        }
        
        // Move the timer in the context by the duration of the note.
        moveTimerByNoteDuration(ctx);

        return notes;
    }

    private void moveTimerByNoteDuration(NodeContext ctx) {
        // adds the note "duration" to timing.This is calculated based on Bpm and Tempo.
        float secondPrBeat = (float) ((1.0/ctx.bpm.bpm)*60.0);    // calculates the time between beats
        float beatsPrNode = (float) ((1.0*ctx.bpm.tempo.toFraction()) / ctx.tempo.toFraction());

        //todo fix accumulating rounding errors here..

        // Bpm 120, 1/4
        // og vi vil spille 1/16 Node
        // secondPrBeat = 0.5s
        // beatsPrNode = (1/4) / (1/16) = 4
        // tid = 0.5s/4 = 1/8s

        ctx.timing += ((float)Math.round((secondPrBeat/beatsPrNode)*1000))/1000;

        //ctx.timing += secondPrBeat/beatsPrNode;
    }

    public List<FinalNote> visit(OctaveChangeNode node, NodeContext ctx){
        // This visit should only change the ctx, and add no new notes.
        ctx.octave += node.getDeltaOctave();
        return new ArrayList<>();   // returns empty list
    }

    public List<FinalNote> visit(RepeatNode node, NodeContext ctx){
        List<FinalNote> notes = new ArrayList<>();

        IntByReference iteration = new IntByReference(1);

        // We add the iteration as a Integer class (reference type), so that the every-nodes can peek at the stack.
        ctx.repeatIterationStack.push(iteration);
        for(; iteration.value <= node.getAmount(); iteration.value++){   // todo tjek at integer er en poienter
            notes.addAll(node.getStmts().accept(this,ctx));
        }
        ctx.repeatIterationStack.pop();

        return notes;
    }

    public List<FinalNote> visit(StmtList node, NodeContext ctx){
        List<FinalNote> notes = new ArrayList<>();

        // Visits all the nodes in the stmtlist
        for(StmtNode stmt : node){
            notes.addAll(stmt.accept(this,ctx));
        }

        return notes;
    }

    public List<FinalNote> visit(TempoChangeNode node, NodeContext ctx){
        // This visit should only change the ctx, and add no new notes.
        ctx.tempo = new Tempo(node.getNumerator(),node.getDenominator());
        return new ArrayList<>();
    }

    public List<FinalNote> visit(TransposeNode node, NodeContext ctx){
        List<FinalNote> toBeTransposed = node.getToBeTransposed().accept(this,ctx);

        for(FinalNote note : toBeTransposed){
            note.transpose(node.getDeltaTonation());
        }

        return toBeTransposed;
    }

}