package fr.sethlans.core.material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import fr.sethlans.core.material.layout.BindingLayout;
import fr.sethlans.core.material.layout.PushConstantLayout;

public class MaterialLayout {

    private final Map<Integer, List<BindingLayout>> setLayouts = new HashMap<>();

    private final List<PushConstantLayout> pushConstantLayouts = new ArrayList<>();

    public MaterialLayout() {
        super();
    }

    public MaterialLayout putBindingsSet(int set, List<BindingLayout> bindings) {
        this.setLayouts.put(set, bindings);
        return this;
    }

    public MaterialLayout addPushConstant(PushConstantLayout pushConstant) {
        this.pushConstantLayouts.add(pushConstant);
        return this;
    }

    public Set<Entry<Integer, List<BindingLayout>>> setLayouts() {
        return setLayouts.entrySet();
    }

    public List<PushConstantLayout> pushConstantLayouts() {
        return pushConstantLayouts;
    }
}
