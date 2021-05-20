package neureka.framing.states;

import lombok.experimental.Accessors;
import neureka.framing.NDFrame;
import neureka.utility.functional.Replace;
import neureka.utility.functional.With;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;


@Accessors( prefix = {"_"})
public final class AxisFrame<GetType, ValueType> {
    
    private final At<Object, Get<GetType>> _keyBasedGetter;
    private final At<Object, Set<ValueType>> _keyBasedSetter;
    private final Replace<Object, Object, NDFrame<ValueType>> _replace;
    private final Supplier<List<Object>> _allAliasGetter;
    private final Function<Integer, List<Object>> _allAliasGetterForIndex;

    private AxisFrame(
            At<Object, Get<GetType>> keyBasedGetter,
            At<Object, Set<ValueType>> keyBasedSetter,
            Replace<Object, Object, NDFrame<ValueType>> replace,
            Supplier<List<Object>> allAliasGetter,
            Function<Integer, List<Object>> allAliasGetterForIndex
    ) {
        this._keyBasedGetter = keyBasedGetter;
        this._keyBasedSetter = keyBasedSetter;
        this._replace = replace;
        this._allAliasGetter = allAliasGetter;
        this._allAliasGetterForIndex = allAliasGetterForIndex;
    }

    public static <SetType, GetType, ValueType> Builder<SetType, GetType, ValueType> builder() {
        return new Builder<>();
    }
 
    public GetType getIndexAtAlias(Object aliasKey) {
        return _keyBasedGetter.at(aliasKey).get();
    }

    public Set<ValueType> atIndexAlias(Object aliasKey) {
        return _keyBasedSetter.at(aliasKey);
    }

    public With<Object, NDFrame<ValueType>> replace(Object indexAlias ) {
        return _replace.replace( indexAlias );
    }

    public List<Object> getAllAliases() {
        return _allAliasGetter.get();
    }
    
    public List<Object> getAllAliasesForIndex( int index ) {
        return _allAliasGetterForIndex.apply( index );
    } 

    public static class Builder<SetType, GetType, ValueType> {
        private At<Object, Get<GetType>> keyBasedGetter;
        private At<Object, Set<ValueType>> keyBasedSetter;
        private Replace<Object, Object, NDFrame<ValueType>> replacer;
        private Supplier<List<Object>> allAliasGetter;
        private Function<Integer, List<Object>> allAliasGetterForIndex;

        Builder() { }

        public Builder<SetType, GetType, ValueType> getter(At<Object, Get<GetType>> keyBasedGetter) {
            this.keyBasedGetter = keyBasedGetter;
            return this;
        }

        public Builder<SetType, GetType, ValueType> setter(At<Object, Set<ValueType>> keyBasedSetter) {
            this.keyBasedSetter = keyBasedSetter;
            return this;
        }

        public Builder<SetType, GetType, ValueType> replacer(Replace<Object, Object, NDFrame<ValueType>> replacer) {
            this.replacer = replacer;
            return this;
        }

        public Builder<SetType, GetType, ValueType> allAliasGetter(Supplier<List<Object>> allAliasGetter) {
            this.allAliasGetter = allAliasGetter;
            return this;
        }

        public Builder<SetType, GetType, ValueType> allAliasGetterFor(Function<Integer, List<Object>> allAliasGetterForIndex) {
            this.allAliasGetterForIndex = allAliasGetterForIndex;
            return this;
        }

        public AxisFrame<GetType, ValueType> build() {
            return new AxisFrame<>(keyBasedGetter, keyBasedSetter, replacer, allAliasGetter, allAliasGetterForIndex); 
        }
 
    }
}
