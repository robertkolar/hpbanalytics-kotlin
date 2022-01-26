/**
 * Created by robertk on 9/6/2015.
 */
Ext.define('HanGui.view.statistics.CurrentStatisticsGrid', {
    extend: 'Ext.grid.Panel',
    xtype: 'han-current-statistics-grid',
    requires: [
        'Ext.grid.column.Date',
        'Ext.toolbar.Paging',
        'HanGui.view.statistics.StatisticsController',
        'Ext.form.field.ComboBox',
        'Ext.form.field.Checkbox'
    ],
    bind: '{currentStatistics}',
    viewConfig: {
        stripeRows: true
    },
    columns: [{
        text: '#',
        width: 60,
        dataIndex: 'id'
    }, {
        text: 'Period',
        width: 100,
        dataIndex: 'periodDate',
        xtype: 'datecolumn',
        format: 'm/d/Y'
    }, {
        text: '#Execs',
        width: 80,
        dataIndex: 'numExecs',
        align: 'right'
    }, {
        text: '#Opn',
        width: 80,
        dataIndex: 'numOpened',
        align: 'right'
    }, {
        text: '#Cls',
        width: 80,
        dataIndex: 'numClosed',
        align: 'right'
    }, {
        text: '#Win',
        width: 80,
        dataIndex: 'numWinners',
        align: 'right'
    }, {
        text: '#Los',
        width: 80,
        dataIndex: 'numLosers',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00%',
        text: 'Win%',
        width: 100,
        dataIndex: 'pctWinners',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'Big W',
        width: 100,
        dataIndex: 'bigWinner',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'Big L',
        width: 100,
        dataIndex: 'bigLoser',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'W Profit',
        width: 100,
        dataIndex: 'winnersProfit',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'L Loss',
        width: 100,
        dataIndex: 'losersLoss',
        align: 'right'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'Value B',
        width: 100,
        dataIndex: 'valueBought',
        align: 'right',
        renderer: 'valueRenderer'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'Value S',
        width: 100,
        dataIndex: 'valueSold',
        align: 'right',
        renderer: 'valueRenderer'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'TValue B',
        width: 100,
        dataIndex: 'timeValueBought',
        align: 'right',
        renderer: 'valueRenderer'
    }, {
        xtype: 'numbercolumn',
        format: '0.00',
        text: 'TValue S',
        width: 100,
        dataIndex: 'timeValueSold',
        align: 'right',
        renderer: 'valueRenderer'
    }, {
        text: 'PL Period',
        width: 100,
        dataIndex: 'profitLoss',
        align: 'right',
        renderer: 'profitLossRenderer'
    }, {
        text: 'PL TaxRep',
        width: 100,
        dataIndex: 'profitLossTaxReport',
        align: 'right',
        renderer: 'profitLossRenderer'
    }, {
        text: 'Cumul PL',
        width: 100,
        dataIndex: 'cumulProfitLoss',
        align: 'right',
        renderer: 'profitLossRenderer'
    }, {
        flex: 1
    }],
    dockedItems: [{
        xtype: 'pagingtoolbar',
        reference: 'currentStatisticsPaging',
        bind: '{currentStatistics}',
        dock: 'bottom',
        displayInfo: true
    }, {
        xtype: 'toolbar',
        items: [{
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'abbr',
            reference: 'cTradeTypeCombo',
            fieldLabel: 'TradeType',
            width: 150,
            labelWidth: 65,
            store: Ext.create('Ext.data.Store', {
                fields: ['abbr', 'name'],
                data: [
                    {"abbr": "ALL", "name": "--All--"},
                    {"abbr": "LONG", "name": "Long"},
                    {"abbr": "SHORT", "name": "Short"}
                ]
            }),
            value: 'ALL',
            margin: '0 0 0 10',
            listeners: {
                change: 'reloadCurrentStatistics'
            }
        }, {
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'abbr',
            reference: 'cSecTypeCombo',
            fieldLabel: 'SecType',
            width: 150,
            labelWidth: 60,
            store: Ext.create('Ext.data.Store', {
                fields: ['abbr', 'name'],
                data: [
                    {"abbr": "ALL", "name": "--All--"},
                    {"abbr": "OPT", "name": "OPT"},
                    {"abbr": "FOP", "name": "FOP"},
                    {"abbr": "FUT", "name": "FUT"},
                    {"abbr": "CFD", "name": "CFD"},
                    {"abbr": "STK", "name": "STK"}
                ]
            }),
            value: 'ALL',
            margin: '0 0 0 10',
            listeners: {
                change: 'reloadCurrentStatistics'
            }
        }, {
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'abbr',
            reference: 'cCurrencyCombo',
            fieldLabel: 'Currency',
            width: 150,
            labelWidth: 60,
            store: Ext.create('Ext.data.Store', {
                fields: ['abbr', 'name'],
                data: [
                    {"abbr": "ALL", "name": "--All--"},
                    {"abbr": "USD", "name": "USD"},
                    {"abbr": "EUR", "name": "EUR"},
                    {"abbr": "CHF", "name": "CHF"},
                    {"abbr": "GBP", "name": "GBP"},
                    {"abbr": "JPY", "name": "JPY"},
                    {"abbr": "AUD", "name": "AUD"},
                    {"abbr": "KRW", "name": "KRW"},
                    {"abbr": "HKD", "name": "HKD"},
                    {"abbr": "SGD", "name": "SGD"}
                ]
            }),
            value: 'ALL',
            margin: '0 0 0 10',
            listeners: {
                change: 'reloadCurrentStatistics'
            }
        }, {
            xtype: 'combobox',
            editable: false,
            queryMode: 'local',
            displayField: 'name',
            valueField: 'abbr',
            reference: 'cUnderlyingCombo',
            fieldLabel: 'Underlying',
            width: 170,
            labelWidth: 70,
            store: Ext.create('Ext.data.ArrayStore', {
                fields: ['abbr', 'name'],
                data: [
                    {"abbr": "ALL", "name": "--All--"}
                ]
            }),
            value: 'ALL',
            margin: '0 0 0 10',
            listeners: {
                change: 'reloadCurrentStatistics'
            }
        }, {
            xtype: 'checkbox',
            reference: 'cOpenOnlyCheckbox',
            fieldLabel: 'O',
            width: 30,
            labelWidth: 10,
            checked : false,
            margin: '0 0 0 10',
            listeners: {
                change: 'prepareCurrentUnderlyingCombo'
            }
        }, {
            xtype: 'button',
            margin: '0 0 0 10',
            text: 'Calculate',
            handler: 'onCalculateCurrentStatistics',
            listeners: {
                beforerender: function(c, eOpts) {
                    c.setGlyph(HanGui.common.Glyphs.getGlyph('gear'));
                }
            }
        }, {
            xtype: 'tbtext',
            flex: 1
        }, {
            xtype: 'tbtext',
            html: 'WS status',
            width: 120,
            margin: '0 0 0 10',
            reference: 'cWsStatus'
        }]
    }]
});
