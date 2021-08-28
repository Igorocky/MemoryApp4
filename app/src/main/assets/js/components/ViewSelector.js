"use strict";

const BOOK_VIEW_BASE_PATH = '/bookView'

const VIEW_NAME_ATTR = '_view'
function createQueryObjectForView(viewName, params) {
    return {[VIEW_NAME_ATTR]:viewName, ...(hasValue(params)?params:{})}
}

const HOME_PAGE_VIEW = 'homePage'
const DEBUG_VIEW = 'debug'
const PAGE_1_VIEW = 'page1'
const PAGE_2_VIEW = 'page2'
const VIEWS = {
    [HOME_PAGE_VIEW]:{
        render({query, openView, setPageTitle}) {
            return re(HomePage,{openView, setPageTitle})
        }
    },
    [DEBUG_VIEW]:{
        render({query, openView, setPageTitle}) {
            return re(DebugPage,{openView, setPageTitle})
        }
    },
    [PAGE_1_VIEW]:{
        render({query, openView, setPageTitle}) {
            return re(Page1,{openView, setPageTitle})
        }
    },
    [PAGE_2_VIEW]:{
        render({query, openView, setPageTitle}) {
            return re(Page2,{openView, setPageTitle})
        }
    },
}

const ViewSelector = ({}) => {
    const [currentViewUrl, setCurrentViewUrl] = useState(null)
    const [environmentName, setEnvironmentName] = useState(null)
    const [pageTitle, setPageTitle] = useState(null)

    const query = parseSearchParams(currentViewUrl)

    useEffect(() => {
        updatePageTitle()
    }, [environmentName, pageTitle])

    function updatePageTitle() {
        document.title = `${environmentName == 'PROD' ? '' : '{' + environmentName + '} - '}${pageTitle}`
    }

    function renderSelectedView() {
        const selectedView = VIEWS[query[VIEW_NAME_ATTR]]??VIEWS[HOME_PAGE_VIEW]
        if (selectedView) {
            return selectedView.render({
                query,
                openView: (viewName,params) => setCurrentViewUrl(window.location.pathname + '?' + new URLSearchParams(createQueryObjectForView(viewName,params)).toString()),
                setPageTitle: str => setPageTitle(str),
            })
        }
    }

    if (currentViewUrl) {
        return RE.Container.col.top.left({}, {},
            // `currentViewUrl: ${currentViewUrl}`,
            renderSelectedView()
        )
    } else {
        const newViewUrl = window.location.pathname + window.location.search
        setCurrentViewUrl(newViewUrl)
        return "Starting..."
    }
}