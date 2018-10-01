package com.highpowerbear.hpbanalytics.rest;

import com.highpowerbear.hpbanalytics.dao.OrdTrackDao;
import com.highpowerbear.hpbanalytics.dao.filter.FilterParser;
import com.highpowerbear.hpbanalytics.dao.filter.IbOrderFilter;
import com.highpowerbear.hpbanalytics.entity.IbAccount;
import com.highpowerbear.hpbanalytics.entity.IbOrder;
import com.highpowerbear.hpbanalytics.ordtrack.HeartbeatControl;
import com.highpowerbear.hpbanalytics.ibclient.IbController;
import com.highpowerbear.hpbanalytics.ordtrack.model.Position;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by robertk on 11/18/2017.
 */
@RestController
@RequestMapping("/ordtrack")
public class OrdTrackRestController {

    private final OrdTrackDao ordTrackDao;
    private final IbController ibController;
    private final FilterParser filterParser;
    private final HeartbeatControl heartbeatControl;

    @Autowired
    public OrdTrackRestController(OrdTrackDao ordTrackDao, IbController ibController, FilterParser filterParser, HeartbeatControl heartbeatControl) {
        this.ordTrackDao = ordTrackDao;
        this.ibController = ibController;
        this.filterParser = filterParser;
        this.heartbeatControl = heartbeatControl;
    }

    @RequestMapping("/ibaccounts")
    public RestList<IbAccount> getIbAccount() {
        List<IbAccount> ibAccounts = ordTrackDao.getIbAccounts();
        ibAccounts.forEach(ibAccount -> ibAccount.setIbConnection(ibController.getIbConnection(ibAccount.getAccountId())));

        return new RestList<>(ibAccounts, (long) ibAccounts.size());
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/ibaccounts")
    public ResponseEntity<?> updateIbAccount(
            @RequestBody IbAccount ibAccount) {

        IbAccount ibAccountDb = ordTrackDao.findIbAccount(ibAccount.getAccountId());
        if (ibAccountDb == null) {
            return ResponseEntity.notFound().build();
        }
        ibAccountDb = ordTrackDao.updateIbAccount(ibAccount);

        return ResponseEntity.ok(ibAccountDb);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/ibaccounts/{accountId}/connect/{connect}")
    public ResponseEntity<?> connectIbAccount(
            @PathVariable("accountId") String accountId,
            @PathVariable("connect") boolean connect) {

        IbAccount ibAccount = ordTrackDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        if (connect) {
            ibController.connect(accountId);
        } else {
            ibController.disconnect(accountId);
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException ie) {
            // Ignore
        }
        ibAccount.setIbConnection(ibController.getIbConnection(accountId));

        return ResponseEntity.ok(ibAccount);
    }

    @RequestMapping("/ibaccounts/{accountId}/iborders")
    public ResponseEntity<?> getFilteredIbOrders(
            @PathVariable("accountId") String accountId,
            @RequestParam(required = false, value = "filter") String jsonFilter,
            @RequestParam("start") Integer start,
            @RequestParam("limit") Integer limit) {

        IbAccount ibAccount = ordTrackDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        List<IbOrder> ibOrders = new ArrayList<>();

        IbOrderFilter filter = filterParser.parseIbOrderFilter(jsonFilter);
        Map<IbOrder, Integer> hm = heartbeatControl.getOpenOrderHeartbeatMap().get(accountId);

        for (IbOrder ibOrder : ordTrackDao.getFilteredIbOrders(accountId, filter, start, limit)) {
            ibOrder.setHeartbeatCount(hm.get(ibOrder));
            ibOrders.add(ibOrder);
        }

        return ResponseEntity.ok(new RestList<>(ibOrders, ordTrackDao.getNumFilteredIbOrders(accountId, filter)));
    }

    @RequestMapping("/ibaccounts/{accountId}/positions")
    public ResponseEntity<?> getPositions(
            @PathVariable("accountId") String accountId,
            @RequestParam("start") Integer start,
            @RequestParam("limit") Integer limit) {

        IbAccount ibAccount = ordTrackDao.findIbAccount(accountId);
        if (ibAccount == null) {
            return ResponseEntity.notFound().build();
        }

        List<Position> positions = ibController.getPositions(accountId);
        if (positions == null) {
            positions = new ArrayList<>();
        }
        int total = positions.size();

        if (!positions.isEmpty()) {
            int fromIndex = Math.min(start, total - 1);
            int toIndex = Math.min(fromIndex + limit, total);
            List<Position> positionsPaged = positions.subList(fromIndex, toIndex);

            return ResponseEntity.ok(new RestList<>(positionsPaged, (long) total));
        } else {
            return ResponseEntity.ok(new RestList<>(positions, (long) total));
        }
    }
}