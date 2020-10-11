/**
 * Created by robertk on 10/15/2015.
 */
Ext.define('HanGui.view.trade.TradeController', {
    extend: 'Ext.app.ViewController',

    requires: [
        'HanGui.common.Definitions',
        'HanGui.view.trade.window.TradeCloseWindow',
        'HanGui.view.trade.window.TradeExecutionWindow',
        'HanGui.view.trade.TradeExecutionGrid'
    ],

    alias: 'controller.han-trade',

    init: function() {
        var me = this,
            trades = me.getStore('trades'),
            wsStatusField = me.lookupReference('wsStatus');

        if (trades) {
            trades.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/trade');
            me.loadTrades();
        }

        var socket  = new SockJS('/websocket');
        var stompClient = Stomp.over(socket);
        stompClient.debug = function(str) {
        };

        stompClient.connect({}, function(frame) {
            console.log('WS trade connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('han-connected');

            stompClient.subscribe('/topic/trade', function(message) {
                if (message.body.startsWith('reloadRequest')) {
                    trades.reload();
                }
            });

        }, function() {
            console.log('WS trade disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('han-connected');
            wsStatusField.addCls('han-disconnected');
        });
    },

    loadTrades: function() {
        var me = this,
            trades = me.getStore('trades');

        trades.load(function(records, operation, success) {
            if (success) {
                console.log('loaded trades');
            }
        });
    },

    onRegenerateAllTrades: function(button) {
        var me = this;

        Ext.Msg.show({
            title:'Regenerate all trades?',
            message: 'All trades will be deleted and regenrated again',
            buttons: Ext.Msg.YESNO,
            icon: Ext.Msg.QUESTION,
            fn: function(btn) {
                if (btn === 'yes') {
                    Ext.Ajax.request({
                        method: 'POST',
                        url: HanGui.common.Definitions.urlPrefix + '/trade/regenerate-all'
                    });
                }
            }
        });
    },

    onCloseTrade: function(button, evt) {
        var me = this,
            trade = button.getWidgetRecord().data;

        var window = Ext.create('HanGui.view.trade.window.TradeCloseWindow', {
            reference: 'tradeCloseWindow',
            title: 'Close Trade, id=' + trade.id
        });
        me.getView().add(window);
        window.trade = trade;
        me.lookupReference('closeDate').setValue(new Date());
        me.lookupReference('closePrice').setValue(0.0);
        window.show();
    },

    onSubmitCloseTrade: function(button, evt) {
        var me = this,
            form = me.lookupReference('tradeCloseForm'),
            window = me.lookupReference('tradeCloseWindow'),
            trade = window.trade;

        var urlString = HanGui.common.Definitions.urlPrefix + '/trade/' + trade.id + '/close';

        if (form && form.isValid()) {
            Ext.Ajax.request({
                method: 'PUT',
                url: urlString,
                jsonData: {
                    closeDate: Ext.Date.format(new Date(form.getForm().findField('closeDate').lastValue), 'Y-m-d\\TH:i:s.u'),
                    closePrice: form.getForm().findField('closePrice').lastValue
                },
                success: function (response, opts) {
                    window.close();
                }
            });
        }
    },

    onCancelCloseTrade: function(button, evt) {
        this.lookupReference('tradeCloseWindow').close();
    },

    showTradeExecutions: function (view, cell, cellIndex, record, row, rowIndex, e) {
        var me = this;

        if (e.position.column.dataIndex !== 'executionIds') {
            return;
        }

        var store = Ext.create('Ext.data.Store', {
            model: 'HanGui.model.Execution'
        });
        store.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/trade/' + record.id + '/executions');

        var window = Ext.create('HanGui.view.trade.window.TradeExecutionWindow');
        window.setTitle("Executions for Trade id=" + record.data.id);

        var grid = Ext.create('HanGui.view.trade.TradeExecutionGrid');
        grid.setStore(store);
        window.add(grid);
        me.getView().add(window);

        store.load(function(records, operation, success) {
            if (success) {
                console.log('loaded trade executions for trade ' + record.id);
                window.show();
            }
        });
    }
});
