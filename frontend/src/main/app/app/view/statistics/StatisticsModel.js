/**
 * Created by robertk on 8/24/2020.
 */
Ext.define('HanGui.view.statistics.StatisticsModel', {
    extend: 'Ext.app.ViewModel',
    requires: [
        'HanGui.model.CurrentStatistics',
        'HanGui.model.Statistics'
    ],

    alias: 'viewmodel.han-statistics',

    stores: {
        currentStatistics: {
            model: 'HanGui.model.CurrentStatistics',
            pageSize: 10
        },
        statistics: {
            model: 'HanGui.model.Statistics',
            pageSize: 10
        },
        charts: {
            model: 'HanGui.model.Chart'
        }
    }
});
