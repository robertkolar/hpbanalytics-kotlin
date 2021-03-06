/**
 * Created by robertk on 8/24/2020.
 */
Ext.define('HanGui.view.execution.ExecutionController', {
    extend: 'Ext.app.ViewController',

    alias: 'controller.han-execution',

    init: function() {
        var me = this,
            executions = me.getStore('executions'),
            wsStatusField = me.lookupReference('wsStatus');

        if (executions) {
            executions.getProxy().setUrl(HanGui.common.Definitions.urlPrefix + '/execution');
            me.loadExecutions();
        }

        var socket  = new WebSocket(HanGui.common.Definitions.wsPrefix + '/execution');

        socket.onopen = function(event) {
            console.log('WS execution connected');
            wsStatusField.update('WS connected');
            wsStatusField.addCls('han-connected');
        };

        socket.onmessage = function(event) {
            if (event.data.startsWith('reloadRequest')) {
                executions.reload();
            }
        };

        socket.onclose = function(event) {
            console.log('WS execution disconnected');

            wsStatusField.update('WS disconnected');
            wsStatusField.removeCls('han-connected');
            wsStatusField.addCls('han-disconnected');
        };
    },

    loadExecutions: function() {
        var me = this,
            executions = me.getStore('executions');

        executions.load(function(records, operation, success) {
            if (success) {
                console.log('loaded executions');
            }
        });
    },

    onAddExecution: function(button, e, options) {
        var me = this,
            window = Ext.create('HanGui.view.execution.window.ExecutionAddWindow');

        me.getView().add(window);
        window.show();
    },

    onSubmitAddExecution: function(button, e, options) {
        var me = this,
            form = me.lookupReference('executionAddForm'),
            window = me.lookupReference('executionAddWindow');

        if (form && form.isValid()) {
            Ext.Ajax.request({
                method: 'POST',
                url: HanGui.common.Definitions.urlPrefix + '/execution',
                jsonData: {
                    reference: form.getForm().findField('reference').lastValue,
                    action: form.getForm().findField('action').lastValue,
                    quantity: form.getForm().findField('quantity').lastValue,
                    symbol: form.getForm().findField('symbol').lastValue,
                    underlying: form.getForm().findField('underlying').lastValue,
                    currency: form.getForm().findField('currency').lastValue,
                    secType: form.getForm().findField('secType').lastValue,
                    multiplier: form.getForm().findField('multiplier').lastValue,
                    fillDate: Ext.Date.format(new Date(form.getForm().findField('fillDate').lastValue), 'Y-m-d\\TH:i:s.u'),
                    fillPrice: form.getForm().findField('fillPrice').lastValue
                },
                success: function(response, opts) {
                    window.close();
                }
            });
        }
    },

    onCancelAddExecution: function(button, e, options) {
        this.lookupReference('executionAddWindow').close();
    },

    onDeleteExecution: function(button) {
        var me = this,
            execution = button.getWidgetRecord().data;

        Ext.Msg.show({
            title:'Delete Execution?',
            message: 'Are you sure you want to delete the execution, id=' + execution.id + '?',
            buttons: Ext.Msg.YESNO,
            icon: Ext.Msg.QUESTION,
            fn: function(btn) {
                if (btn === 'yes') {
                    Ext.Ajax.request({
                        method: 'DELETE',
                        url: HanGui.common.Definitions.urlPrefix + '/execution/' + execution.id
                    });
                }
            }
        });
    },

    actionRenderer: function(val, metadata, record) {
        metadata.style = (val === 'BUY' ? 'color: blue;' : 'color: brown;');
        return val;
    },

    valueRenderer: function(val, metadata, record) {
        metadata.style = (val > 0 ? 'color: blue;' : 'color: brown;');
        return Ext.util.Format.number(val, '0.00');
    },

    priceRenderer: function(val, metadata, record) {
        return Ext.util.Format.number(val, '0.00###');
    }
});
