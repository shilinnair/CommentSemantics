public class X {

    void foo() {
        FlowInfo mergedInfo = // merging two true initInfos for such a negative case: if ((t && (b = t)) || f) r = b; // b may not have been initialized
        FlowInfo.conditional(leftInfo.initsWhenTrue().copy().unconditionalInits().mergedWith(rightInfo.initsWhenTrue().copy().unconditionalInits()), falseMergedInfo);
        mergedInitStateIndex = currentScope.methodScope().recordInitializationStates(mergedInfo);
        return mergedInfo;
    }
}
