package com.alipay.simplehbase.antlr.manual;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.filter.FilterList;
import org.apache.hadoop.hbase.filter.FilterList.Operator;

import org.apache.hadoop.hbase.filter.SingleColumnValueFilter;

import com.alipay.simplehbase.antlr.auto.StatementsParser.AndconditionContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.CidContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.ConditioncContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.ConstantContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.EqualconstantContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.EqualvarContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.GreaterconstantContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.GreatervarContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.LessconstantContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.LessvarContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.OrconditionContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.ProgContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.SelectcContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.VarContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.WherecContext;
import com.alipay.simplehbase.antlr.auto.StatementsParser.WrapperContext;
import com.alipay.simplehbase.antlr.auto.StatementsVisitor;
import com.alipay.simplehbase.config.HBaseColumnSchema;
import com.alipay.simplehbase.config.HBaseTableConfig;
import com.alipay.simplehbase.util.Util;

/**
 * SimpleHbaseVisitor.
 * 
 * @author xinzhi.zhang
 * */
public class SimpleHbaseVisitor implements StatementsVisitor<Filter> {

    private HBaseTableConfig    hbaseTableConfig;
    private Map<String, Object> para;

    public SimpleHbaseVisitor(HBaseTableConfig hbaseTableConfig,
            Map<String, Object> para) {
        this.hbaseTableConfig = hbaseTableConfig;
        this.para = para;
    }

    @Override
    public Filter visitOrcondition(OrconditionContext ctx) {
        List<ConditioncContext> conditioncContextList = ctx.conditionc();
        List<Filter> filters = new ArrayList<Filter>();
        for (ConditioncContext conditioncContext : conditioncContextList) {
            filters.add(conditioncContext.accept(this));
        }

        FilterList filterList = new FilterList(Operator.MUST_PASS_ONE, filters);
        return filterList;
    }

    @Override
    public Filter visitAndcondition(AndconditionContext ctx) {
        List<ConditioncContext> conditioncContextList = ctx.conditionc();
        List<Filter> filters = new ArrayList<Filter>();
        for (ConditioncContext conditioncContext : conditioncContextList) {
            filters.add(conditioncContext.accept(this));
        }

        FilterList filterList = new FilterList(Operator.MUST_PASS_ALL, filters);
        return filterList;
    }

    @Override
    public Filter visitWrapper(WrapperContext ctx) {
        return ctx.conditionc().accept(this);
    }

    @Override
    public Filter visitEqualvar(EqualvarContext ctx) {
        CidContext cidContext = ctx.cid();
        VarContext varContext = ctx.var();
        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parsePara(varContext, para);

        return constructFilter(hbaseColumnSchema, CompareOp.EQUAL, object);
    }

    @Override
    public Filter visitEqualconstant(EqualconstantContext ctx) {
        CidContext cidContext = ctx.cid();
        ConstantContext constantContext = ctx.constant();

        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parseConstant(hbaseColumnSchema,
                constantContext);

        return constructFilter(hbaseColumnSchema, CompareOp.EQUAL, object);
    }

    @Override
    public Filter visitLessvar(LessvarContext ctx) {

        CidContext cidContext = ctx.cid();
        VarContext varContext = ctx.var();

        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parsePara(varContext, para);

        return constructFilter(hbaseColumnSchema, CompareOp.LESS, object);
    }

    @Override
    public Filter visitLessconstant(LessconstantContext ctx) {
        CidContext cidContext = ctx.cid();
        ConstantContext constantContext = ctx.constant();

        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parseConstant(hbaseColumnSchema,
                constantContext);

        return constructFilter(hbaseColumnSchema, CompareOp.LESS, object);

    }

    @Override
    public Filter visitGreaterconstant(GreaterconstantContext ctx) {
        CidContext cidContext = ctx.cid();
        ConstantContext constantContext = ctx.constant();

        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parseConstant(hbaseColumnSchema,
                constantContext);

        return constructFilter(hbaseColumnSchema, CompareOp.GREATER, object);
    }

    @Override
    public Filter visitGreatervar(GreatervarContext ctx) {
        CidContext cidContext = ctx.cid();
        VarContext varContext = ctx.var();

        HBaseColumnSchema hbaseColumnSchema = ContextUtil
                .parseHBaseColumnSchema(hbaseTableConfig, cidContext);
        Object object = ContextUtil.parsePara(varContext, para);
        return constructFilter(hbaseColumnSchema, CompareOp.GREATER, object);
    }

    private static Filter constructFilter(HBaseColumnSchema hbaseColumnSchema,
            CompareOp compareOp, Object object) {
        byte[] value = hbaseColumnSchema.getTypeHandler().toBytes(
                hbaseColumnSchema.getType(), object);

        Util.checkNull(value);

        byte[] familyBytes = hbaseColumnSchema.getFamilyBytes();
        byte[] qualifierBytes = hbaseColumnSchema.getQualifierBytes();

        SingleColumnValueFilter singleColumnValueFilter = new SingleColumnValueFilter(
                familyBytes, qualifierBytes, compareOp, value);
        singleColumnValueFilter.setFilterIfMissing(true);

        return singleColumnValueFilter;

    }

    @Override
    public Filter visitProg(ProgContext ctx) {
        return null;
    }

    @Override
    public Filter visitSelectc(SelectcContext ctx) {
        return null;
    }

    @Override
    public Filter visitCid(CidContext ctx) {
        return null;
    }

    @Override
    public Filter visitWherec(WherecContext ctx) {
        return null;
    }

    @Override
    public Filter visitVar(VarContext ctx) {
        return null;
    }

    @Override
    public Filter visitConstant(ConstantContext ctx) {
        return null;
    }

    @Override
    public Filter visit(@NotNull ParseTree arg0) {
        return null;
    }

    @Override
    public Filter visitChildren(@NotNull RuleNode arg0) {
        return null;
    }

    @Override
    public Filter visitErrorNode(@NotNull ErrorNode arg0) {
        return null;
    }

    @Override
    public Filter visitTerminal(@NotNull TerminalNode arg0) {
        return null;
    }

}