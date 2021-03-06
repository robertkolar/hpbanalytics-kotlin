/**
 * Created by robertk on 10/15/2015.
 */
Ext.define('HanGui.view.statistics.StatisticsController', {
    extend: 'Ext.app.ViewController',

    requires: [
        'HanGui.common.Definitions',
        'Ext.data.proxy.Proxy'
    ],

    alias: 'controller.han-statistics',

    init: function() {
        var me = this,
            currentStatistics = me.getStore('currentStatistics'),
            statistics = me.getStore('statistics'),
            charts = me.getStore('charts'),
            cWsStatusField = me.lookupReference('cWsStatus'),
            wsStatusField = me.lookupReference('wsStatus');

        if (currentStatistics) {
            currentStatistics.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/statistics/current');
            me.reloadCurrentStatistics();
        }

        if (statistics) {
            statistics.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/statistics');
            charts.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/statistics/charts');
            me.reloadStatisticsAndCharts();
        }

        me.prepareCurrentUnderlyingCombo();
        me.prepareUnderlyingCombo();
        me.prepareIfiYearMonthCombos();

        var socketCurStat  = new WebSocket(HanGui.common.Definitions.wsPrefix + '/current-statistics');

        socketCurStat.onopen = function(event) {
            console.log('WS current-statistics connected');
            cWsStatusField.update('WS connected');
            cWsStatusField.addCls('han-connected');
        };

        socketCurStat.onmessage = function(event) {
            if (event.data.startsWith('reloadRequest')) {
                me.reloadCurrentStatistics();
            }
        };

        socketCurStat.onclose = function(event) {
            console.log('WS current-statistics disconnected');

            cWsStatusField.update('WS disconnected');
            cWsStatusField.removeCls('han-connected');
            cWsStatusField.addCls('han-disconnected');
        };

        var socketStat  = new WebSocket(HanGui.common.Definitions.wsPrefix + '/statistics');

        socketStat.onopen = function(event) {
            console.log('WS statistics connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('han-connected');
        };

        socketStat.onmessage = function(event) {
            if (event.data.startsWith('reloadRequest')) {
                me.reloadStatisticsAndCharts();
            }
        };

        socketStat.onclose = function(event) {
            console.log('WS statistics disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('han-connected');
            wsStatusField.addCls('han-disconnected');
        };
    },

    reloadCurrentStatistics: function() {
        var me = this,
            currentStatistics = me.getStore('currentStatistics'),
            proxy = currentStatistics.getProxy(),
            tradeType  = me.lookupReference('cTradeTypeCombo').getValue(),
            secType  = me.lookupReference('cSecTypeCombo').getValue(),
            currency  = me.lookupReference('cCurrencyCombo').getValue(),
            underlying =  me.lookupReference('cUnderlyingCombo').getValue();

        proxy.setExtraParam('tradeType', tradeType);
        proxy.setExtraParam('secType', secType);
        proxy.setExtraParam('currency', currency);
        proxy.setExtraParam('underlying', underlying);

        currentStatistics.load(function(records, operation, success) {
            if (success) {
                console.log('reloaded current statistics for underlying=' + underlying);
            }
        });
    },

    reloadStatisticsAndCharts: function() {
        var me = this,
            statistics = me.getStore('statistics'),
            charts = me.getStore('charts'),
            statisticsProxy = statistics.getProxy(),
            chartsProxy = charts.getProxy(),
            interval = me.lookupReference('intervalCombo').getValue(),
            tradeType  = me.lookupReference('tradeTypeCombo').getValue(),
            secType  = me.lookupReference('secTypeCombo').getValue(),
            currency  = me.lookupReference('currencyCombo').getValue(),
            underlying =  me.lookupReference('underlyingCombo').getValue(),
            statisticsPaging = me.lookupReference('statisticsPaging');

        me.lookupReference('chartsButton').toggle(false);

        statisticsProxy.setExtraParam('interval', interval);
        chartsProxy.setExtraParam('interval', interval);

        statisticsProxy.setExtraParam('tradeType', tradeType);
        chartsProxy.setExtraParam('tradeType', tradeType);

        statisticsProxy.setExtraParam('secType', secType);
        chartsProxy.setExtraParam('secType', secType);

        statisticsProxy.setExtraParam('currency', currency);
        chartsProxy.setExtraParam('currency', currency);

        statisticsProxy.setExtraParam('underlying', underlying);
        chartsProxy.setExtraParam('underlying', underlying);

        if (statisticsPaging.getStore().isLoaded()) {
            statisticsPaging.moveFirst();
        } else {
            statistics.load(function(records, operation, success) {
                if (success) {
                    console.log('reloaded statistics for interval=' + interval + ', underlying=' + underlying);
                }
            });
        }
        charts.load(function(records, operation, success) {
            if (success) {
                console.log('reloaded charts for interval=' + interval + ', underlying=' + underlying);
            }
        });
    },

    prepareCurrentUnderlyingCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('cUnderlyingCombo'),
            openOnlyCheckbox = me.lookupReference('cOpenOnlyCheckbox');

        me.populateUnderlyingCombo(underlyingCombo, openOnlyCheckbox.getValue());
    },

    prepareUnderlyingCombo: function() {
        var me = this,
            underlyingCombo =  me.lookupReference('underlyingCombo'),
            openOnlyCheckbox = me.lookupReference('openOnlyCheckbox');

        me.populateUnderlyingCombo(underlyingCombo, openOnlyCheckbox.getValue());
    },

    populateUnderlyingCombo: function(combo, openOnly) {
        Ext.Ajax.request({
            method: 'GET',
            url: HanGui.common.Definitions.urlPrefix + '/statistics/underlyings',
            params: {'openOnly': openOnly},

            success: function(response, opts) {
                var undls = Ext.decode(response.responseText);
                var undlsData = [];
                undlsData.push(['ALL', '--All--']);
                for (var i = 0; i < undls.length; i++) {
                    undlsData.push([undls[i], undls[i]]);
                }
                combo.getStore().loadData(undlsData);
                combo.setValue('ALL');
            }
        });
    },

    prepareIfiYearMonthCombos: function() {
        var me = this,
            ifiYearCombo =  me.lookupReference('ifiYearCombo'),
            ifiEndMonthCombo =  me.lookupReference('ifiEndMonthCombo');

            // years
            var ifiYearsData = [];
            var currentYear = new Date().getFullYear();
            for (var y = 2016; y <= currentYear; y++) {
                ifiYearsData.push([y]);
            }
            ifiYearCombo.getStore().loadData(ifiYearsData);
            ifiYearCombo.setValue(currentYear);

            // months
            var ifiEndMonthsData = [];
            for (var m = 1; m <= 12; m++) {
                ifiEndMonthsData.push([m]);
            }
            ifiEndMonthCombo.getStore().loadData(ifiEndMonthsData);
            var defaultEndMonth = ifiEndMonthCombo.getStore().getAt(ifiEndMonthsData.length - 1);
            ifiEndMonthCombo.setValue(defaultEndMonth);
    },

    onCalculateCurrentStatistics: function(button, evt) {
        var me = this,
            tradeType =  me.lookupReference('cTradeTypeCombo').getValue(),
            secType =  me.lookupReference('cSecTypeCombo').getValue(),
            currency =  me.lookupReference('cCurrencyCombo').getValue(),
            underlying =  me.lookupReference('cUnderlyingCombo').getValue();

        Ext.Ajax.request({
            method: 'POST',
            url: HanGui.common.Definitions.urlPrefix + '/statistics/current',
            jsonData: {
                tradeType: tradeType,
                secType: secType,
                currency: currency,
                underlying: underlying
            }
        });
    },

    onCalculateStatistics: function(button, evt) {
        var me = this,
            interval = me.lookupReference('intervalCombo').getValue(),
            tradeType =  me.lookupReference('tradeTypeCombo').getValue(),
            secType =  me.lookupReference('secTypeCombo').getValue(),
            currency =  me.lookupReference('currencyCombo').getValue(),
            underlying =  me.lookupReference('underlyingCombo').getValue();

        Ext.Ajax.request({
            method: 'POST',
            url: HanGui.common.Definitions.urlPrefix + '/statistics',
            jsonData: {
                interval: interval,
                tradeType: tradeType,
                secType: secType,
                currency: currency,
                underlying: underlying
            }
        });
    },

    onDownloadIfiReport: function(button, evt) {
        var me = this,
            year = me.lookupReference('ifiYearCombo').getValue(),
            endMonth = me.lookupReference('ifiEndMonthCombo').getValue(),
            tradeType =  me.lookupReference('ifiTradeTypeCombo').getValue();

        Ext.Ajax.request({
            method: 'GET',
            url: HanGui.common.Definitions.urlPrefix + '/statistics/ifi/csv',
            params: {
                year: year,
                endMonth: endMonth,
                tradeType: tradeType
            },

            success: function(response, opts) {
                var content = response.responseText;

                var filename;
                if (endMonth !== 12) {
                    var endMonthStr = '' + endMonth;
                    var padMonth = '00';
                    endMonthStr = padMonth.substring(0, padMonth.length - endMonthStr.length) + endMonthStr;
                    filename = 'IFI_' + year + '_' + endMonthStr + '_' + tradeType.toLowerCase() + '.csv';
                } else {
                    filename = 'IFI_' + year + '_'  + tradeType.toLowerCase() + '.csv';
                }

                var blob = new Blob([content], {
                    type: 'text/plain;charset=utf-8'
                });
                saveAs(blob, filename);
            }
        });
    },

    createCharts: function(tabPanel, newCard, oldCard, eOpts) {
        var me = this,
            statistics = me.getStore('charts'),

            cumulativePl = [],
            profitLoss = [],
            timeValueBoughtSold = [],
            timeValueSum = [],
            numberExecutions = [],
            numberOpenedClosed = [],
            numberWinnersLosers = [],
            pctWinners = [],
            bigWinnerLoser = [],
            plWinnersLosers = [];

        if (!Ext.get('hpb_c1')) {
            return;
        }
        cumulativePl.push(['Date', 'Cumulative PL']);
        profitLoss.push(['Date', 'PL', {role: 'style'}]);
        timeValueBoughtSold.push(['Date', 'Time Value Bought', 'Time Value Sold']);
        timeValueSum.push(['Date', 'Time Value Sum', {role: 'style'}]);
        numberExecutions.push(['Date', 'Executions']);
        numberOpenedClosed.push(['Date', 'Opened', 'Closed']);
        numberWinnersLosers.push(['Date', 'Winners', 'Losers']);
        pctWinners.push(['Date', 'Percent Winners']);
        bigWinnerLoser.push(['Date', 'Big Winner', 'Big Loser']);
        plWinnersLosers.push(['Date', 'Winners Profit', 'Losers Loss']);

        statistics.each(function (record, id) {
            var rd = record.data;

            cumulativePl.push([new Date(rd.periodDate), rd.cumulProfitLoss]);
            profitLoss.push([new Date(rd.periodDate), rd.profitLoss, (rd.profitLoss > 0 ? 'green' : (rd.profitLoss === 0 ? 'white' : 'red'))]);
            timeValueBoughtSold.push([new Date(rd.periodDate), rd.timeValueBought, -rd.timeValueSold]);
            timeValueSum.push([new Date(rd.periodDate), rd.timeValueSum, (rd.timeValueSum > 0 ? 'blue' : (rd.timeValueSum === 0 ? 'white' : 'brown'))]);
            numberExecutions.push([new Date(rd.periodDate), rd.numExecs]);
            numberOpenedClosed.push([new Date(rd.periodDate), rd.numOpened, rd.numClosed]);
            numberWinnersLosers.push([new Date(rd.periodDate), rd.numWinners, rd.numLosers]);
            pctWinners.push([new Date(rd.periodDate), rd.pctWinners]);
            bigWinnerLoser.push([new Date(rd.periodDate), rd.bigWinner, rd.bigLoser]);
            plWinnersLosers.push([new Date(rd.periodDate), rd.winnersProfit, rd.losersLoss]);
        });

        GoogleChart.ceateLineChart(cumulativePl, 'Cumulative PL', 'hpb_c1');
        GoogleChart.ceateColumnChart(profitLoss, 'Profit/Loss', 'hpb_c2');
        GoogleChart.ceateColumnChartCustomColor(timeValueBoughtSold, 'Time Value Bought/Sold', 'hpb_c3', 'blue', 'brown');
        GoogleChart.ceateColumnChart(timeValueSum, 'Time Value Sum', 'hpb_c4');
        GoogleChart.ceateColumnChart(numberExecutions, 'Number Executions', 'hpb_c5');
        GoogleChart.ceateColumnChart(numberOpenedClosed, 'Number Opened/Closed', 'hpb_c6');
        GoogleChart.ceateColumnChart(numberWinnersLosers, 'Number Winners/Losers', 'hpb_c7');
        GoogleChart.ceateColumnChart(pctWinners, 'Percent Winners', 'hpb_c8');
        GoogleChart.ceateColumnChart(bigWinnerLoser, 'Biggest Winner/Loser', 'hpb_c9');
        GoogleChart.ceateColumnChart(plWinnersLosers, 'Winners Profit/Losers Loss', 'hpb_c10');
    },

    onChartsToggle: function(button, pressed, eOpts) {
        var me = this,
            chartsContainer = me.lookupReference('chartsContainer');

        if (pressed) {
            chartsContainer.setVisible(true);
            me.createCharts();
        } else {
            chartsContainer.setVisible(false);
        }
    },

    valueRenderer: function(val, metadata, record) {
        metadata.style = (val > 0 ? 'color: blue;' : 'color: brown;');
        return Ext.util.Format.number(val, '0.00');
    },

    profitLossRenderer: function(val, metadata, record) {
        metadata.style = val < 0 ? 'color: red;' : 'color: green;';
        return Ext.util.Format.number(val, '0.00');
    }
});
