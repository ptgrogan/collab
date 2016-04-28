clear all
clc

g = NameGenerator();

numberDesigners = 3;
trainingFactories = [
    TaskFactory('Training #1 (Individual)', '1x1 SU TU', [1 1 1], [0 0 0], ...
        {0 1 2}, {0 1 2}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Training #2 (Individual)', '2x2 SU TU', [2 2 2], [0 0 0], ...
        {0:1 2:3 4:5}, {0:1 2:3 4:5}, g.getInputNames(6), g.getOutputNames(6))
    TaskFactory('Training #3 (Individual)', '2x2 SU TC', [2 2 2], [1 1 1], ...
        {0:1 2:3 4:5}, {0:1 2:3 4:5}, g.getInputNames(6), g.getOutputNames(6))
    TaskFactory('Training #4 (Team)', '3x3 SC TU', 3, 0, ...
        {0 1 2}, {2 0 1}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Training #5 (Team)', '3x3 SC TC', 3, 1, ...
        {0 1 2}, {0 1 2}, g.getInputNames(3), g.getOutputNames(3))
    ];
experimentFactories = [
    TaskFactory('Breezy Rain (Individual)', '3x3 SU TU', [3 3 3], [0 0 0], ...
        {0:2 3:5 6:8}, {0:2 3:5 6:8}, g.getInputNames(9), g.getOutputNames(9))
%     TaskFactory('Impolite Heat (Individual)', '3x3 SU TU', [3 3 3], [0 0 0], ... % added 4/24
%         {0:2 3:5 6:8}, {0:2 3:5 6:8}, g.getInputNames(9), g.getOutputNames(9))
    TaskFactory('Chief Government (Individual)', '4x4 SU TU', [4 4 4], [0 0 0], ...
        {0:3 4:7 8:11}, {0:3 4:7 8:11}, g.getInputNames(12), g.getOutputNames(12))
%     TaskFactory('Muddled Reward (Individual)', '4x4 SU TU', [4 4 4], [0 0 0], ... % added 4/24
%         {0:3 4:7 8:11}, {0:3 4:7 8:11}, g.getInputNames(12), g.getOutputNames(12))
    TaskFactory('Thinkable Ink (Individual)', '6x6 SU TU', [6 6 6], [0 0 0], ...
        {0:5 6:11 12:17}, {0:5 6:11 12:17}, g.getInputNames(18), g.getOutputNames(18))
%     TaskFactory('Crabby Example (Individual)', '6x6 SU TU', [6 6 6], [0 0 0], ... % added 4/24
%         {0:5 6:11 12:17}, {0:5 6:11 12:17}, g.getInputNames(18), g.getOutputNames(18))
    TaskFactory('Hallowed Sign (Individual)', '2x2 SU TC', [2 2 2], [1 1 1], ...
        {0:1 2:3 4:5}, {0:1 2:3 4:5}, g.getInputNames(6), g.getOutputNames(6))
    TaskFactory('Husky Verse (Individual)', '2x2 SU TC', [2 2 2], [1 1 1], ... % added 4/24
        {0:1 2:3 4:5}, {0:1 2:3 4:5}, g.getInputNames(6), g.getOutputNames(6))
    TaskFactory('Statuesque Name (Individual)', '3x3 SU TC', [3 3 3], [1 1 1], ...
        {0:2 3:5 6:8}, {0:2 3:5 6:8}, g.getInputNames(9), g.getOutputNames(9))
    TaskFactory('Flat Sleep (Individual)', '3x3 SU TC', [3 3 3], [1 1 1], ... % added 4/24
        {0:2 3:5 6:8}, {0:2 3:5 6:8}, g.getInputNames(9), g.getOutputNames(9))
    TaskFactory('Brainy Damage (Individual)', '4x4 SU TC', [4 4 4], [1 1 1], ...
        {0:3 4:7 8:11}, {0:3 4:7 8:11}, g.getInputNames(12), g.getOutputNames(12))
    TaskFactory('Silky Waste (Individual)', '4x4 SU TC', [4 4 4], [1 1 1], ... % added 4/24
        {0:3 4:7 8:11}, {0:3 4:7 8:11}, g.getInputNames(12), g.getOutputNames(12))
    TaskFactory('Murky Mass (Team)', '2x2 SPC TC (A)', 2, 1, ...
        {0 1 []}, {0 1 []}, g.getInputNames(2), g.getOutputNames(2))
    TaskFactory('Wistful Act (Team)', '2x2 SPC TC (B)', 2, 1, ...
        {[] 0 1}, {[] 0 1}, g.getInputNames(2), g.getOutputNames(2))
    TaskFactory('Unwritten Experience (Team)', '2x2 SPC TC (C)', 2, 1, ...
        {0 [] 1}, {0 [] 1}, g.getInputNames(2), g.getOutputNames(2))
    TaskFactory('Onerous Effect (Team)', '3x3 SPC TU (A)', 3, 0, ...
        {0 1 2}, {1 0 2}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Hard Development (Team)', '3x3 SPC TU (B)', 3, 0, ...
        {0 1 2}, {0 2 1}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Alert Burst (Team)', '3x3 SPC TU (C)', 3, 0, ...
        {0 1 2}, {2 1 0}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Befitting Plant (Team)', '3x3 SPC TC (A)', 3, 1, ...
        {[0 1] 2 []}, {[0 1] 2 []}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Arrogant Flame (Team)', '3x3 SPC TC (B)', 3, 1, ...        % added 4/24
        {[] [0 1] 2}, {[] [0 1] 2}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Better Behavior (Team)', '3x3 SPC TC (C)', 3, 1, ...       % added 4/24
        {0 [] [1 2]}, {0 [] [1 2]}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Absorbed Copper (Team)', '4x4 SPC TC', 4, 1, ...
        {[0 1] [2 3] []}, {[0 1] [2 3] []}, g.getInputNames(4), g.getOutputNames(4))
%     TaskFactory([g.getTaskName() ' (Team)'], '4x4 SPC TC (B)', 4, 1, ...
%         {[] [0 1] [2 3]}, {[] [0 1] [2 3]}, g.getInputNames(4), g.getOutputNames(4))
%     TaskFactory([g.getTaskName() ' (Team)'], '4x4 SPC TC (C)', 4, 1, ...
%         {[0 1] [] [2 3]}, {[0 1] [] [2 3]}, g.getInputNames(4), g.getOutputNames(4))
    TaskFactory('Towering Test (Team)', '3x3 SC TU (A)', 3, 0, ...
        {0 1 2}, {2 0 1}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Staking System (Team)', '3x3 SC TU (B)', 3, 0, ...
        {0 1 2}, {1 2 0}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Wide Growth (Team)', '6x6 SC TU', 6, 0, ...
        {[0 1] [2 3] [4 5]}, {[2 4] [0 5] [1 3]}, g.getInputNames(6), g.getOutputNames(6))
    TaskFactory('Economic Motion (Team)', '3x3 SC TC ', 3, 1, ...
        {0 1 2}, {0 1 2}, g.getInputNames(3), g.getOutputNames(3))
%     TaskFactory('Chemical Rhythm (Team)', '3x3 SC TC (B)', 3, 1, ...    % added 4/24
%         {0 1 2}, {0 1 2}, g.getInputNames(3), g.getOutputNames(3))
    TaskFactory('Noiseless Stone (Team)', '4x4 SC TC', 4, 1, ...
        {[0 1] 2 3}, {[0 1] 2 3}, g.getInputNames(4), g.getOutputNames(4))
%     TaskFactory([g.getTaskName() ' (Team)'], '4x4 SC TC (B)', 4, 1, ...
%         {0 [1 2] 3}, {0 [1 2] 3}, g.getInputNames(4), g.getOutputNames(4))
%     TaskFactory([g.getTaskName() ' (Team)'], '4x4 SC TC (C)', 4, 1, ...
%         {0 1 [2 3]}, {0 1 [2 3]}, g.getInputNames(4), g.getOutputNames(4))
    ];

experid = fopen('experiments.txt','w');
fprintf(experid, '%2s %35s %16s %4s %4s %4s\n', ...
    '#', 'Name (Individual/Team)', 'Description', 'C_t', 'C_s', 'C');
for i=1:length(experimentFactories)
    sample = experimentFactories(i).generate();
    fprintf(experid, '%2d %35s %16s %4.f %4.f %4.f\n', ...
        i, sample.name, sample.desc, sample.C_t, sample.C_s, sample.C);
end

for e=5:12
    name = ['experiment' sprintf('%3.3i',e)];
    fprintf(experid,'\nexperiment%3.3i: [',e);
    
    % generate training models
    trainingModels = SystemModel.empty(length(trainingFactories),0);
    for i=1:length(trainingFactories)
        trainingModels(i) = trainingFactories(i).generate();
    end

    % generate experiment models
    experimentModels = SystemModel.empty(length(experimentFactories),0);
    for i=1:length(experimentFactories)
        experimentModels(i) = experimentFactories(i).generate();
    end
    
    % randomize ordering of experiment models
    randOrder = randperm(length(experimentModels));
    % redraw if tasks 8, 9, 19, or 24 (4x4 TC) are among first 10 tasks
    while any(ismember([8 9 19 24],randOrder(1:10)))
        randOrder = randperm(length(experimentModels));
    end

    json = ['{"name":"' name '",'];
    json = [json '"numberDesigners":' num2str(numberDesigners) ','];
    json = [json '"trainingModels":['];
    for i=1:length(trainingModels)
        model = trainingModels(i);
        json = [json jsonGenerator(model)];
        if i < length(trainingModels)
            json = [json ','];
        end
    end
    json = [json '],'];
    json = [json '"experimentModels":['];
    for i=1:length(randOrder)
        model = experimentModels(randOrder(i));
        fprintf(experid,' %i',randOrder(i));
        json = [json jsonGenerator(model)];
        if i < length(experimentModels)
            json = [json ','];
        end
    end
    fprintf(experid,'%s\n',' ]');
    json = [json ']'];
    json = [json '}'];
    
    % save json-formatted data for use in application
    fileid = fopen(['experiment' sprintf('%3.3i',e) '.json'],'w');
    fprintf(fileid,'%s',json);
    fclose(fileid);
    
    % save matlab-formatted data for post-processing
    save(['experiment' sprintf('%3.3i',e) '.mat'], 'name', ...
        'numberDesigners', 'trainingModels', 'experimentModels');
end
fclose(experid);