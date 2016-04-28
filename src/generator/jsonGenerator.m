function json = jsonGenerator(model)

json = '{';
json = [json '"name":"' model.name '",'];
json = [json '"couplingMatrix":['];
for i = 1:length(model.M)
    json = [json '['];
    for j = 1:length(model.M(i,:))
        json = [json num2str(model.M(i,j))];
        if j < length(model.M(i,:))
            json = [json ','];
        end
    end
    json = [json ']'];
    if i < length(model.M)
        json = [json ','];
    end
end
json = [json '],'];
json = [json '"targetVector":['];
for i = 1:length(model.y_star)
    json = [json num2str(model.y_star(i))];
    if i < length(model.y_star)
        json = [json ','];
    end
end
json = [json '],'];
json = [json '"inputIndices":['];
for i = 1:length(model.inputs)
    json = [json '['];
    for j = 1:length(model.inputs{i})
        json = [json num2str(model.inputs{i}(j))];
        if j < length(model.inputs{i})
            json = [json ','];
        end
    end
    json = [json ']'];
    if i < length(model.inputs)
        json = [json ','];
    end
end
json = [json '],'];
json = [json '"outputIndices":['];
for i = 1:length(model.outputs)
    json = [json '['];
    for j = 1:length(model.outputs{i})
        json = [json num2str(model.outputs{i}(j))];
        if j < length(model.outputs{i})
            json = [json ','];
        end
    end
    json = [json ']'];
    if i < length(model.outputs)
        json = [json ','];
    end
end
json = [json '],'];
json = [json '"inputLabels":['];
for i = 1:length(model.inputLabels)
    json = [json num2str(model.inputLabels{i})];
    if i < length(model.inputLabels)
        json = [json ','];
    end
end
json = [json '],'];
json = [json '"outputLabels":['];
for i = 1:length(model.outputLabels)
    json = [json num2str(model.outputLabels{i})];
    if i < length(model.outputLabels)
        json = [json ','];
    end
end
json = [json ']'];
json = [json '}'];