(() => {
    const {node, setNotice, message} = window.CsasUi;

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.reports = async () => {
        const select = document.getElementById('report-group');
        const link = document.getElementById('group-rating-report');
        const pdfLink = document.getElementById('group-rating-pdf-report');
        select.addEventListener('change', () => {
            if (!select.value) {
                link.hidden = true;
                pdfLink.hidden = true;
                return;
            }
            link.href = `/api/reports/groups/${select.value}/ratings.xlsx`;
            pdfLink.href = `/api/reports/groups/${select.value}/ratings.pdf`;
            link.hidden = false;
            pdfLink.hidden = false;
        });
        try {
            const groups = await window.CsasApi.request('/api/groups');
            groups.forEach(group => select.append(node('option', {value: group.id}, group.name)));
            setNotice(document.querySelector('[data-global-notice]'), `Групп: ${groups.length}.`);
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
